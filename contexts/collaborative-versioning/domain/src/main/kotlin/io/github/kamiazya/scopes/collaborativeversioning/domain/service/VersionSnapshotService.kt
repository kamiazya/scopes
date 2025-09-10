package io.github.kamiazya.scopes.collaborativeversioning.domain.service

import arrow.core.Either
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.Snapshot
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SnapshotServiceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.TrackedResource
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceContent
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.SnapshotId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionNumber
import kotlinx.datetime.Instant

/**
 * Domain service for managing version snapshots.
 *
 * This service handles the creation and restoration of resource snapshots,
 * providing state-based versioning capabilities for tracked resources.
 * It ensures data integrity through comprehensive validation and error handling.
 */
interface VersionSnapshotService {

    /**
     * Create a new snapshot for a tracked resource.
     *
     * @param resource The tracked resource to snapshot
     * @param content The content to snapshot
     * @param authorId The author creating the snapshot
     * @param message A descriptive message for the snapshot
     * @param metadata Optional metadata for the snapshot
     * @param timestamp The timestamp for the snapshot
     * @return Either an error or the created snapshot
     */
    suspend fun createSnapshot(
        resource: TrackedResource,
        content: ResourceContent,
        authorId: AgentId,
        message: String,
        metadata: Map<String, String> = emptyMap(),
        timestamp: Instant,
    ): Either<SnapshotServiceError, Snapshot>

    /**
     * Restore a resource from a specific snapshot.
     *
     * This operation creates a new snapshot with the content from the target snapshot,
     * maintaining the version history while allowing rollback to previous states.
     *
     * @param resource The tracked resource to restore
     * @param targetSnapshotId The snapshot to restore from
     * @param authorId The author performing the restoration
     * @param message A descriptive message for the restoration
     * @param timestamp The timestamp for the restoration
     * @return Either an error or the new snapshot created from the restoration
     */
    suspend fun restoreSnapshot(
        resource: TrackedResource,
        targetSnapshotId: SnapshotId,
        authorId: AgentId,
        message: String,
        timestamp: Instant,
    ): Either<SnapshotServiceError, Snapshot>

    /**
     * Restore a resource to a specific version number.
     *
     * @param resource The tracked resource to restore
     * @param targetVersion The version number to restore to
     * @param authorId The author performing the restoration
     * @param message A descriptive message for the restoration
     * @param timestamp The timestamp for the restoration
     * @return Either an error or the new snapshot created from the restoration
     */
    suspend fun restoreToVersion(
        resource: TrackedResource,
        targetVersion: VersionNumber,
        authorId: AgentId,
        message: String,
        timestamp: Instant,
    ): Either<SnapshotServiceError, Snapshot>

    /**
     * Get a snapshot by its ID.
     *
     * @param resourceId The resource ID
     * @param snapshotId The snapshot ID
     * @return Either an error or the snapshot (null if not found)
     */
    suspend fun getSnapshot(resourceId: ResourceId, snapshotId: SnapshotId): Either<SnapshotServiceError, Snapshot?>

    /**
     * Get all snapshots for a resource.
     *
     * @param resourceId The resource ID
     * @return Either an error or a list of snapshots ordered by version
     */
    suspend fun getSnapshots(resourceId: ResourceId): Either<SnapshotServiceError, List<Snapshot>>

    /**
     * Calculate the size of a snapshot in bytes.
     *
     * @param snapshot The snapshot to measure
     * @return The size in bytes
     */
    fun calculateSnapshotSize(snapshot: Snapshot): Long

    /**
     * Validate that a snapshot can be created.
     *
     * @param resource The tracked resource
     * @param content The content to validate
     * @return Either an error or Unit on success
     */
    suspend fun validateSnapshot(resource: TrackedResource, content: ResourceContent): Either<SnapshotServiceError, Unit>
}
