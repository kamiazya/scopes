package io.github.kamiazya.scopes.collaborativeversioning.domain.error

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ChangesetId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Error types for find operations.
 */
sealed class FindChangesetError : CollaborativeVersioningError() {
    data class QueryTimeout(val operation: String, val timeoutMs: Long, override val occurredAt: Instant = Clock.System.now()) : FindChangesetError()
    data class IndexCorruption(val changesetId: ChangesetId, val message: String, override val occurredAt: Instant = Clock.System.now()) : FindChangesetError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = Clock.System.now()) : FindChangesetError()
}

/**
 * Error types for save operations.
 */
sealed class SaveChangesetError : CollaborativeVersioningError() {
    data class ConcurrentModification(
        val changesetId: ChangesetId,
        val expectedVersion: Int,
        val actualVersion: Int,
        override val occurredAt: Instant = Clock.System.now(),
    ) : SaveChangesetError()
    data class ValidationFailed(val violations: List<String>, override val occurredAt: Instant = Clock.System.now()) : SaveChangesetError()
    data class StorageQuotaExceeded(val currentSize: Long, val maxSize: Long, override val occurredAt: Instant = Clock.System.now()) : SaveChangesetError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = Clock.System.now()) : SaveChangesetError()
}

/**
 * Error types for apply operations.
 */
sealed class ApplyChangesetError : CollaborativeVersioningError() {
    data class TargetVersionNotFound(val versionId: VersionId, override val occurredAt: Instant = Clock.System.now()) : ApplyChangesetError()
    data class IncompatibleChangeset(val reason: String, override val occurredAt: Instant = Clock.System.now()) : ApplyChangesetError()
    data class ConflictDetected(val conflicts: List<String>, override val occurredAt: Instant = Clock.System.now()) : ApplyChangesetError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = Clock.System.now()) : ApplyChangesetError()
}

/**
 * Error types for exists operations.
 */
sealed class ExistsChangesetError : CollaborativeVersioningError() {
    data class QueryTimeout(val operation: String, val timeoutMs: Long, override val occurredAt: Instant = Clock.System.now()) : ExistsChangesetError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = Clock.System.now()) : ExistsChangesetError()
}

/**
 * Error types for TrackedResource find operations.
 */
sealed class FindTrackedResourceError : CollaborativeVersioningError() {
    data class QueryTimeout(val operation: String, val timeoutMs: Long, override val occurredAt: Instant = Clock.System.now()) : FindTrackedResourceError()
    data class IndexCorruption(val resourceId: ResourceId, val message: String, override val occurredAt: Instant = Clock.System.now()) :
        FindTrackedResourceError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = Clock.System.now()) : FindTrackedResourceError()
    data class DataCorruption(val resourceId: ResourceId, val reason: String, override val occurredAt: Instant = Clock.System.now()) :
        FindTrackedResourceError()
}

/**
 * Error types for TrackedResource save operations.
 */
sealed class SaveTrackedResourceError : CollaborativeVersioningError() {
    data class ConcurrentModification(
        val resourceId: ResourceId,
        val expectedVersion: Int,
        val actualVersion: Int,
        override val occurredAt: Instant = Clock.System.now(),
    ) : SaveTrackedResourceError()
    data class ValidationFailed(val violations: List<String>, override val occurredAt: Instant = Clock.System.now()) : SaveTrackedResourceError()
    data class StorageQuotaExceeded(val currentSize: Long, val maxSize: Long, override val occurredAt: Instant = Clock.System.now()) :
        SaveTrackedResourceError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = Clock.System.now()) : SaveTrackedResourceError()
    data class SnapshotSaveFailed(val resourceId: ResourceId, val reason: String, override val occurredAt: Instant = Clock.System.now()) :
        SaveTrackedResourceError()
}

/**
 * Error types for TrackedResource delete operations.
 */
sealed class DeleteTrackedResourceError : CollaborativeVersioningError() {
    data class ResourceInUse(val resourceId: ResourceId, val usedBy: List<String>, override val occurredAt: Instant = Clock.System.now()) :
        DeleteTrackedResourceError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = Clock.System.now()) : DeleteTrackedResourceError()
    data class PermissionDenied(val resourceId: ResourceId, val reason: String, override val occurredAt: Instant = Clock.System.now()) :
        DeleteTrackedResourceError()
}

/**
 * Error types for TrackedResource exists operations.
 */
sealed class ExistsTrackedResourceError : CollaborativeVersioningError() {
    data class QueryTimeout(val operation: String, val timeoutMs: Long, override val occurredAt: Instant = Clock.System.now()) : ExistsTrackedResourceError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = Clock.System.now()) : ExistsTrackedResourceError()
}

/**
 * Error types for ChangeProposal find operations.
 */
sealed class FindChangeProposalError : CollaborativeVersioningError() {
    data class QueryTimeout(val operation: String, val timeoutMs: Long, override val occurredAt: Instant = Clock.System.now()) : FindChangeProposalError()
    data class IndexCorruption(val proposalId: ProposalId, val message: String, override val occurredAt: Instant = Clock.System.now()) :
        FindChangeProposalError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = Clock.System.now()) : FindChangeProposalError()
    data class DataCorruption(val proposalId: ProposalId, val reason: String, override val occurredAt: Instant = Clock.System.now()) : FindChangeProposalError()
}

/**
 * Error types for ChangeProposal save operations.
 */
sealed class SaveChangeProposalError : CollaborativeVersioningError() {
    data class ConcurrentModification(
        val proposalId: ProposalId,
        val expectedVersion: Int,
        val actualVersion: Int,
        override val occurredAt: Instant = Clock.System.now(),
    ) : SaveChangeProposalError()
    data class ValidationFailed(val violations: List<String>, override val occurredAt: Instant = Clock.System.now()) : SaveChangeProposalError()
    data class StorageQuotaExceeded(val currentSize: Long, val maxSize: Long, override val occurredAt: Instant = Clock.System.now()) :
        SaveChangeProposalError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = Clock.System.now()) : SaveChangeProposalError()
    data class StateTransitionConflict(
        val proposalId: ProposalId,
        val currentState: String,
        val attemptedState: String,
        override val occurredAt: Instant = Clock.System.now(),
    ) : SaveChangeProposalError()
}

/**
 * Error types for ChangeProposal delete operations.
 */
sealed class DeleteChangeProposalError : CollaborativeVersioningError() {
    data class ProposalInUse(val proposalId: ProposalId, val reason: String, override val occurredAt: Instant = Clock.System.now()) :
        DeleteChangeProposalError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = Clock.System.now()) : DeleteChangeProposalError()
    data class PermissionDenied(val proposalId: ProposalId, val reason: String, override val occurredAt: Instant = Clock.System.now()) :
        DeleteChangeProposalError()
    data class ProposalNotFound(val proposalId: ProposalId, override val occurredAt: Instant = Clock.System.now()) : DeleteChangeProposalError()
}

/**
 * Error types for ChangeProposal exists operations.
 */
sealed class ExistsChangeProposalError : CollaborativeVersioningError() {
    data class QueryTimeout(val operation: String, val timeoutMs: Long, override val occurredAt: Instant = Clock.System.now()) : ExistsChangeProposalError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = Clock.System.now()) : ExistsChangeProposalError()
}
