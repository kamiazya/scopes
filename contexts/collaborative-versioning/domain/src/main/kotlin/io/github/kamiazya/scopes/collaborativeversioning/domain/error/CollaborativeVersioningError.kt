package io.github.kamiazya.scopes.collaborativeversioning.domain.error

import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ChangesetId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalState
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.SnapshotId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionNumber

/**
 * Base error class for Collaborative Versioning bounded context.
 * All errors in this context should extend this class.
 */
sealed class CollaborativeVersioningError

/**
 * Errors related to Resource ID validation.
 */
sealed class ResourceIdError : CollaborativeVersioningError() {
    data class InvalidFormat(val providedValue: String, val expectedFormat: String) : ResourceIdError()
}

/**
 * Errors related to Version ID validation.
 */
sealed class VersionIdError : CollaborativeVersioningError() {
    data class InvalidFormat(val providedValue: String, val expectedFormat: String) : VersionIdError()
}

/**
 * Errors related to Changeset ID validation.
 */
sealed class ChangesetIdError : CollaborativeVersioningError() {
    data class InvalidFormat(val providedValue: String, val expectedFormat: String) : ChangesetIdError()
}

/**
 * Errors related to version operations.
 */
sealed class VersionError : CollaborativeVersioningError() {
    data class VersionNotFound(val versionId: VersionId) : VersionError()

    data class ResourceNotFound(val resourceId: ResourceId) : VersionError()

    data class InvalidVersionSequence(val resourceId: ResourceId, val expectedVersion: Int, val actualVersion: Int) : VersionError()

    data class VersionAlreadyExists(val versionId: VersionId) : VersionError()
}

/**
 * Errors related to changeset operations.
 */
sealed class ChangesetError : CollaborativeVersioningError() {
    data class ChangesetNotFound(val changesetId: ChangesetId) : ChangesetError()

    data class InvalidChangeset(val changesetId: ChangesetId, val reason: String) : ChangesetError()

    data class ChangesetAlreadyApplied(val changesetId: ChangesetId, val appliedAt: Instant) : ChangesetError()

    data class ConflictingChanges(val changesetId: ChangesetId, val conflictingChangesets: List<ChangesetId>) : ChangesetError()
}

/**
 * Errors related to merge operations.
 */
sealed class MergeError : CollaborativeVersioningError() {
    data class IncompatibleVersions(val sourceVersion: VersionId, val targetVersion: VersionId, val reason: String) : MergeError()

    data class UnresolvedConflicts(val conflicts: List<ConflictDetail>) : MergeError()

    data class MergeInProgress(val resourceId: ResourceId, val startedBy: AgentId, val startedAt: Instant) : MergeError()
}

/**
 * Detail about a specific conflict.
 */
data class ConflictDetail(val path: String, val sourceValue: String?, val targetValue: String?, val description: String)

/**
 * Errors related to VersionNumber validation.
 */
sealed class VersionNumberError : CollaborativeVersioningError() {
    data class InvalidValue(val providedValue: Int, val reason: String) : VersionNumberError()
}

/**
 * Errors related to ResourceContent validation.
 */
sealed class ResourceContentError : CollaborativeVersioningError() {
    data class EmptyContent() : ResourceContentError()

    data class ContentTooLarge(val actualSize: Int, val maxSize: Int) : ResourceContentError()

    data class InvalidJson(val reason: String, val cause: Throwable? = null) : ResourceContentError()
}

/**
 * Errors related to Snapshot ID validation.
 */
sealed class SnapshotIdError : CollaborativeVersioningError() {
    data class InvalidFormat(val providedValue: String, val expectedFormat: String) : SnapshotIdError()
}

/**
 * Errors related to TrackedResource operations.
 */
sealed class TrackedResourceError : CollaborativeVersioningError() {
    data class NoSnapshots(val resourceId: ResourceId) : TrackedResourceError()

    data class NoChangeHistory(val resourceId: ResourceId) : TrackedResourceError()

    data class VersionNotFound(val resourceId: ResourceId, val versionNumber: VersionNumber) : TrackedResourceError()

    data class InvalidRestore(val resourceId: ResourceId, val currentVersion: VersionNumber, val targetVersion: VersionNumber) : TrackedResourceError()
}

/**
 * Errors related to TrackedResourceService operations.
 */
sealed class TrackedResourceServiceError : CollaborativeVersioningError() {
    data class ResourceMismatch(val expected: ResourceId, val actual: ResourceId) : TrackedResourceServiceError()

    data class InvalidContent(val reason: String) : TrackedResourceServiceError()
}

/**
 * Errors related to Proposal ID validation.
 */
sealed class ProposalIdError : CollaborativeVersioningError() {
    data class InvalidFormat(val providedValue: String, val expectedFormat: String) : ProposalIdError()
}

/**
 * Errors related to Author validation.
 */
sealed class AuthorError : CollaborativeVersioningError() {
    data object EmptyId : AuthorError()

    data object EmptyDisplayName : AuthorError()

    data class InvalidUserId(val providedValue: String) : AuthorError()

    data class InvalidAgentId(val providedValue: String) : AuthorError()
}

/**
 * Errors related to ChangeProposal operations.
 */
sealed class ChangeProposalError : CollaborativeVersioningError() {
    data object EmptyTitle : ChangeProposalError()

    data object EmptyDescription : ChangeProposalError()

    data object EmptyRejectionReason : ChangeProposalError()

    data object NoProposedChanges : ChangeProposalError()

    data class InvalidStateTransition(val currentState: ProposalState, val attemptedAction: String) : ChangeProposalError()

    data class ResourceMismatch(val expected: ResourceId, val actual: ResourceId) : ChangeProposalError()

    data class ProposedChangeNotFound(val proposedChangeId: String) : ChangeProposalError()

    data class ParentCommentNotFound(val parentCommentId: String) : ChangeProposalError()

    data class ProposalNotFound(val proposalId: ProposalId) : ChangeProposalError()

    data class ConflictDetected(val conflicts: List<String>) : ChangeProposalError()
}

/**
 * Errors related to SnapshotService operations.
 */
sealed class SnapshotServiceError : CollaborativeVersioningError() {
    data class SnapshotNotFound(val resourceId: ResourceId, val snapshotId: SnapshotId) : SnapshotServiceError()

    data class VersionNotFound(val resourceId: ResourceId, val versionNumber: VersionNumber) : SnapshotServiceError()

    data class InvalidContent(val reason: String) : SnapshotServiceError()

    data class ResourceMismatch(val expected: ResourceId, val actual: ResourceId) : SnapshotServiceError()

    data class StorageLimitExceeded(val currentSize: Long, val maxSize: Long) : SnapshotServiceError()

    data class SerializationError(val reason: String, val cause: Throwable? = null) : SnapshotServiceError()

    data class DeserializationError(val reason: String, val cause: Throwable? = null) : SnapshotServiceError()

    data class InvalidRestoreTarget(val resourceId: ResourceId, val currentVersion: VersionNumber, val targetVersion: VersionNumber) : SnapshotServiceError()

    data class MetadataValidationError(val key: String, val value: String, val reason: String) : SnapshotServiceError()
}
