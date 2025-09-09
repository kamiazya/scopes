package io.github.kamiazya.scopes.collaborativeversioning.domain.error

import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ChangesetId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionNumber
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Base error class for Collaborative Versioning bounded context.
 * All errors in this context should extend this class.
 */
sealed class CollaborativeVersioningError {
    abstract val occurredAt: Instant
}

/**
 * Errors related to Resource ID validation.
 */
sealed class ResourceIdError : CollaborativeVersioningError() {
    data class InvalidFormat(val providedValue: String, val expectedFormat: String, override val occurredAt: Instant = Clock.System.now()) : ResourceIdError()
}

/**
 * Errors related to Version ID validation.
 */
sealed class VersionIdError : CollaborativeVersioningError() {
    data class InvalidFormat(val providedValue: String, val expectedFormat: String, override val occurredAt: Instant = Clock.System.now()) : VersionIdError()
}

/**
 * Errors related to Changeset ID validation.
 */
sealed class ChangesetIdError : CollaborativeVersioningError() {
    data class InvalidFormat(val providedValue: String, val expectedFormat: String, override val occurredAt: Instant = Clock.System.now()) : ChangesetIdError()
}

/**
 * Errors related to version operations.
 */
sealed class VersionError : CollaborativeVersioningError() {
    data class VersionNotFound(val versionId: VersionId, override val occurredAt: Instant = Clock.System.now()) : VersionError()

    data class ResourceNotFound(val resourceId: ResourceId, override val occurredAt: Instant = Clock.System.now()) : VersionError()

    data class InvalidVersionSequence(
        val resourceId: ResourceId,
        val expectedVersion: Int,
        val actualVersion: Int,
        override val occurredAt: Instant = Clock.System.now(),
    ) : VersionError()

    data class VersionAlreadyExists(val versionId: VersionId, override val occurredAt: Instant = Clock.System.now()) : VersionError()
}

/**
 * Errors related to changeset operations.
 */
sealed class ChangesetError : CollaborativeVersioningError() {
    data class ChangesetNotFound(val changesetId: ChangesetId, override val occurredAt: Instant = Clock.System.now()) : ChangesetError()

    data class InvalidChangeset(val changesetId: ChangesetId, val reason: String, override val occurredAt: Instant = Clock.System.now()) : ChangesetError()

    data class ChangesetAlreadyApplied(val changesetId: ChangesetId, val appliedAt: Instant, override val occurredAt: Instant = Clock.System.now()) :
        ChangesetError()

    data class ConflictingChanges(
        val changesetId: ChangesetId,
        val conflictingChangesets: List<ChangesetId>,
        override val occurredAt: Instant = Clock.System.now(),
    ) : ChangesetError()
}

/**
 * Errors related to merge operations.
 */
sealed class MergeError : CollaborativeVersioningError() {
    data class IncompatibleVersions(
        val sourceVersion: VersionId,
        val targetVersion: VersionId,
        val reason: String,
        override val occurredAt: Instant = Clock.System.now(),
    ) : MergeError()

    data class UnresolvedConflicts(val conflicts: List<ConflictDetail>, override val occurredAt: Instant = Clock.System.now()) : MergeError()

    data class MergeInProgress(
        val resourceId: ResourceId,
        val startedBy: AgentId,
        val startedAt: Instant,
        override val occurredAt: Instant = Clock.System.now(),
    ) : MergeError()
}

/**
 * Detail about a specific conflict.
 */
data class ConflictDetail(val path: String, val sourceValue: String?, val targetValue: String?, val description: String)

/**
 * Errors related to VersionNumber validation.
 */
sealed class VersionNumberError : CollaborativeVersioningError() {
    data class InvalidValue(val providedValue: Int, val reason: String, override val occurredAt: Instant = Clock.System.now()) : VersionNumberError()
}

/**
 * Errors related to ResourceContent validation.
 */
sealed class ResourceContentError : CollaborativeVersioningError() {
    data class EmptyContent(override val occurredAt: Instant = Clock.System.now()) : ResourceContentError()

    data class ContentTooLarge(val actualSize: Int, val maxSize: Int, override val occurredAt: Instant = Clock.System.now()) : ResourceContentError()

    data class InvalidJson(val reason: String, val cause: Throwable? = null, override val occurredAt: Instant = Clock.System.now()) : ResourceContentError()
}

/**
 * Errors related to Snapshot ID validation.
 */
sealed class SnapshotIdError : CollaborativeVersioningError() {
    data class InvalidFormat(val providedValue: String, val expectedFormat: String, override val occurredAt: Instant = Clock.System.now()) : SnapshotIdError()
}

/**
 * Errors related to TrackedResource operations.
 */
sealed class TrackedResourceError : CollaborativeVersioningError() {
    data class NoSnapshots(val resourceId: ResourceId, override val occurredAt: Instant = Clock.System.now()) : TrackedResourceError()

    data class NoChangeHistory(val resourceId: ResourceId, override val occurredAt: Instant = Clock.System.now()) : TrackedResourceError()

    data class VersionNotFound(val resourceId: ResourceId, val versionNumber: VersionNumber, override val occurredAt: Instant = Clock.System.now()) :
        TrackedResourceError()

    data class InvalidRestore(
        val resourceId: ResourceId,
        val currentVersion: VersionNumber,
        val targetVersion: VersionNumber,
        override val occurredAt: Instant = Clock.System.now(),
    ) : TrackedResourceError()
}

/**
 * Errors related to TrackedResourceService operations.
 */
sealed class TrackedResourceServiceError : CollaborativeVersioningError() {
    data class ResourceMismatch(val expected: ResourceId, val actual: ResourceId, override val occurredAt: Instant = Clock.System.now()) :
        TrackedResourceServiceError()

    data class InvalidContent(val reason: String, override val occurredAt: Instant = Clock.System.now()) : TrackedResourceServiceError()
}
