package io.github.kamiazya.scopes.collaborativeversioning.domain.model

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.github.guepardoapps.kulid.ULID
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.ResourceChange
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.Snapshot
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.TrackedResourceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.SystemTimeProvider
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.*
import kotlinx.datetime.Instant

/**
 * Aggregate root for version-tracked resources.
 *
 * TrackedResource manages the versioning of Scope, Aspect, and Alias entities,
 * maintaining their history through snapshots and tracking changes between versions.
 * This aggregate ensures consistency of version history and provides operations
 * for creating snapshots, recording changes, and querying version history.
 */
class TrackedResource private constructor(
    val id: ResourceId,
    val resourceType: ResourceType,
    var currentVersion: VersionNumber,
    var currentVersionId: VersionId,
    private val snapshots: MutableList<Snapshot>,
    private val changeHistory: MutableList<ResourceChange>,
    val createdAt: Instant,
    var updatedAt: Instant,
) {
    companion object {
        /**
         * Create a new TrackedResource with initial content.
         */
        fun create(
            resourceType: ResourceType,
            initialContent: ResourceContent,
            authorId: AgentId,
            message: String,
            timestamp: Instant = SystemTimeProvider().now(),
        ): TrackedResource {
            val resourceId = ResourceId.generate()
            val versionId = VersionId.generate()
            val versionNumber = VersionNumber.INITIAL

            val initialSnapshot = Snapshot(
                id = SnapshotId.generate(),
                resourceId = resourceId,
                versionId = versionId,
                versionNumber = versionNumber,
                content = initialContent,
                authorId = authorId,
                message = message,
                createdAt = timestamp,
            )

            val initialChange = ResourceChange(
                id = generateChangeId(),
                resourceId = resourceId,
                fromVersionId = null,
                toVersionId = versionId,
                fromVersionNumber = null,
                toVersionNumber = versionNumber,
                authorId = authorId,
                changeType = ResourceChangeType.CREATE,
                message = message,
                createdAt = timestamp,
            )

            return TrackedResource(
                id = resourceId,
                resourceType = resourceType,
                currentVersion = versionNumber,
                currentVersionId = versionId,
                snapshots = mutableListOf(initialSnapshot),
                changeHistory = mutableListOf(initialChange),
                createdAt = timestamp,
                updatedAt = timestamp,
            )
        }

        /**
         * Restore a TrackedResource from existing data.
         */
        fun restore(
            id: ResourceId,
            resourceType: ResourceType,
            currentVersion: VersionNumber,
            currentVersionId: VersionId,
            snapshots: List<Snapshot>,
            changeHistory: List<ResourceChange>,
            createdAt: Instant,
            updatedAt: Instant,
        ): Either<TrackedResourceError, TrackedResource> = either {
            ensure(snapshots.isNotEmpty()) {
                TrackedResourceError.NoSnapshots(id)
            }

            ensure(changeHistory.isNotEmpty()) {
                TrackedResourceError.NoChangeHistory(id)
            }

            TrackedResource(
                id = id,
                resourceType = resourceType,
                currentVersion = currentVersion,
                currentVersionId = currentVersionId,
                snapshots = snapshots.toMutableList(),
                changeHistory = changeHistory.toMutableList(),
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
        }

        private fun generateChangeId(): String = "change_${ULID.random()}"
    }

    /**
     * Create a new snapshot for the resource, incrementing the version.
     */
    fun createSnapshot(
        content: ResourceContent,
        authorId: AgentId,
        message: String,
        timestamp: Instant = SystemTimeProvider().now(),
    ): Either<TrackedResourceError, Snapshot> = either {
        val newVersionNumber = currentVersion.increment()
        val newVersionId = VersionId.generate()

        val snapshot = Snapshot(
            id = SnapshotId.generate(),
            resourceId = id,
            versionId = newVersionId,
            versionNumber = newVersionNumber,
            content = content,
            authorId = authorId,
            message = message,
            createdAt = timestamp,
        )

        val change = ResourceChange(
            id = generateChangeId(),
            resourceId = id,
            fromVersionId = currentVersionId,
            toVersionId = newVersionId,
            fromVersionNumber = currentVersion,
            toVersionNumber = newVersionNumber,
            authorId = authorId,
            changeType = ResourceChangeType.UPDATE,
            message = message,
            createdAt = timestamp,
        )

        recordChange(change, snapshot, timestamp)
        snapshot
    }

    /**
     * Restore the resource to a previous version.
     */
    fun restoreToVersion(
        targetVersionNumber: VersionNumber,
        authorId: AgentId,
        message: String,
        timestamp: Instant = SystemTimeProvider().now(),
    ): Either<TrackedResourceError, Snapshot> = either {
        val targetSnapshot = getSnapshot(targetVersionNumber)
        ensureNotNull(targetSnapshot) {
            TrackedResourceError.VersionNotFound(id, targetVersionNumber)
        }

        ensure(targetVersionNumber.isBefore(currentVersion)) {
            TrackedResourceError.InvalidRestore(id, currentVersion, targetVersionNumber)
        }

        val newVersionNumber = currentVersion.increment()
        val newVersionId = VersionId.generate()

        val restoredSnapshot = Snapshot(
            id = SnapshotId.generate(),
            resourceId = id,
            versionId = newVersionId,
            versionNumber = newVersionNumber,
            content = targetSnapshot.content,
            authorId = authorId,
            message = message,
            createdAt = timestamp,
            metadata = mapOf("restored_from_version" to targetVersionNumber.toString()),
        )

        val change = ResourceChange(
            id = generateChangeId(),
            resourceId = id,
            fromVersionId = currentVersionId,
            toVersionId = newVersionId,
            fromVersionNumber = currentVersion,
            toVersionNumber = newVersionNumber,
            authorId = authorId,
            changeType = ResourceChangeType.RESTORE,
            message = message,
            createdAt = timestamp,
            metadata = mapOf("restored_from_version" to targetVersionNumber.toString()),
        )

        recordChange(change, restoredSnapshot, timestamp)
        restoredSnapshot
    }

    /**
     * Get a snapshot by version number.
     */
    fun getSnapshot(versionNumber: VersionNumber): Snapshot? = snapshots.find { it.versionNumber == versionNumber }

    /**
     * Get a snapshot by version ID.
     */
    fun getSnapshotByVersionId(versionId: VersionId): Snapshot? = snapshots.find { it.versionId == versionId }

    /**
     * Get the current snapshot.
     */
    fun getCurrentSnapshot(): Snapshot? = snapshots.find { it.versionNumber == currentVersion }

    /**
     * Get all snapshots in version order.
     */
    fun getAllSnapshots(): List<Snapshot> = snapshots.sortedBy { it.versionNumber.value }

    /**
     * Get changes since a specific version.
     */
    fun getChangesSince(versionNumber: VersionNumber): List<ResourceChange> = changeHistory.filter { change ->
        change.toVersionNumber.isAfter(versionNumber)
    }.sortedBy { it.toVersionNumber.value }

    /**
     * Get all changes in chronological order.
     */
    fun getAllChanges(): List<ResourceChange> = changeHistory.sortedBy { it.createdAt }

    /**
     * Get changes by author.
     */
    fun getChangesByAuthor(authorId: AgentId): List<ResourceChange> = changeHistory.filter { it.authorId == authorId }
        .sortedBy { it.createdAt }

    /**
     * Check if a specific version exists.
     */
    fun hasVersion(versionNumber: VersionNumber): Boolean = snapshots.any { it.versionNumber == versionNumber }

    /**
     * Get the total number of versions.
     */
    fun getVersionCount(): Int = snapshots.size

    /**
     * Get the history size in bytes.
     */
    fun getHistorySizeInBytes(): Long = snapshots.sumOf { it.contentSizeInBytes().toLong() }

    private fun recordChange(change: ResourceChange, snapshot: Snapshot, timestamp: Instant) {
        changeHistory.add(change)
        snapshots.add(snapshot)
        // Update mutable state - in a real implementation, this would trigger domain events
        currentVersion = snapshot.versionNumber
        currentVersionId = snapshot.versionId
        updatedAt = timestamp
    }
}
