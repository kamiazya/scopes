package io.github.kamiazya.scopes.collaborativeversioning.domain.error

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ChangesetId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionId

/**
 * Error types for find operations.
 */
sealed class FindChangesetError : CollaborativeVersioningError() {
    data class QueryTimeout(val operation: String, val timeoutMs: Long) : FindChangesetError()
    data class IndexCorruption(val changesetId: ChangesetId) : FindChangesetError()
    data class NetworkError(val cause: Throwable?) : FindChangesetError()
}

/**
 * Error types for save operations.
 */
sealed class SaveChangesetError : CollaborativeVersioningError() {
    data class ConcurrentModification(val changesetId: ChangesetId, val expectedVersion: Int, val actualVersion: Int) : SaveChangesetError()
    data class ValidationFailed(val violations: List<String>) : SaveChangesetError()
    data class StorageQuotaExceeded(val currentSize: Long, val maxSize: Long) : SaveChangesetError()
    data class NetworkError(val cause: Throwable?) : SaveChangesetError()
}

/**
 * Error types for apply operations.
 */
sealed class ApplyChangesetError : CollaborativeVersioningError() {
    data class TargetVersionNotFound(val versionId: VersionId) : ApplyChangesetError()
    data class IncompatibleChangeset(val reason: String) : ApplyChangesetError()
    data class ConflictDetected(val conflicts: List<String>) : ApplyChangesetError()
    data class NetworkError(val cause: Throwable?) : ApplyChangesetError()
}

/**
 * Error types for exists operations.
 */
sealed class ExistsChangesetError : CollaborativeVersioningError() {
    data class QueryTimeout(val operation: String, val timeoutMs: Long) : ExistsChangesetError()
    data class NetworkError(val cause: Throwable?) : ExistsChangesetError()
}

/**
 * Error types for TrackedResource find operations.
 */
sealed class FindTrackedResourceError : CollaborativeVersioningError() {
    data class QueryTimeout(val operation: String, val timeoutMs: Long) : FindTrackedResourceError()
    data class IndexCorruption(val resourceId: ResourceId, ) : FindTrackedResourceError()
    data class NetworkError(val cause: Throwable?) : FindTrackedResourceError()
    data class DataCorruption(val resourceId: ResourceId, val reason: String) : FindTrackedResourceError()
}

/**
 * Error types for TrackedResource save operations.
 */
sealed class SaveTrackedResourceError : CollaborativeVersioningError() {
    data class ConcurrentModification(val resourceId: ResourceId, val expectedVersion: Int, val actualVersion: Int) : SaveTrackedResourceError()
    data class ValidationFailed(val violations: List<String>) : SaveTrackedResourceError()
    data class StorageQuotaExceeded(val currentSize: Long, val maxSize: Long) : SaveTrackedResourceError()
    data class NetworkError(val cause: Throwable?) : SaveTrackedResourceError()
    data class SnapshotSaveFailed(val resourceId: ResourceId, val reason: String) : SaveTrackedResourceError()
}

/**
 * Error types for TrackedResource delete operations.
 */
sealed class DeleteTrackedResourceError : CollaborativeVersioningError() {
    data class ResourceInUse(val resourceId: ResourceId, val usedBy: List<String>) : DeleteTrackedResourceError()
    data class NetworkError(val cause: Throwable?) : DeleteTrackedResourceError()
    data class PermissionDenied(val resourceId: ResourceId, val reason: String) : DeleteTrackedResourceError()
}

/**
 * Error types for TrackedResource exists operations.
 */
sealed class ExistsTrackedResourceError : CollaborativeVersioningError() {
    data class QueryTimeout(val operation: String, val timeoutMs: Long) : ExistsTrackedResourceError()
    data class NetworkError(val cause: Throwable?) : ExistsTrackedResourceError()
}

/**
 * Error types for ChangeProposal find operations.
 */
sealed class FindChangeProposalError : CollaborativeVersioningError() {
    data class QueryTimeout(val operation: String, val timeoutMs: Long) : FindChangeProposalError()
    data class IndexCorruption(val proposalId: ProposalId, ) : FindChangeProposalError()
    data class NetworkError(val cause: Throwable?) : FindChangeProposalError()
    data class DataCorruption(val proposalId: ProposalId, val reason: String) : FindChangeProposalError()
}

/**
 * Error types for ChangeProposal save operations.
 */
sealed class SaveChangeProposalError : CollaborativeVersioningError() {
    data class ConcurrentModification(val proposalId: ProposalId, val expectedVersion: Int, val actualVersion: Int) : SaveChangeProposalError()
    data class ValidationFailed(val violations: List<String>) : SaveChangeProposalError()
    data class StorageQuotaExceeded(val currentSize: Long, val maxSize: Long) : SaveChangeProposalError()
    data class NetworkError(val cause: Throwable?) : SaveChangeProposalError()
    data class StateTransitionConflict(val proposalId: ProposalId, val currentState: String, val attemptedState: String) : SaveChangeProposalError()
}

/**
 * Error types for ChangeProposal delete operations.
 */
sealed class DeleteChangeProposalError : CollaborativeVersioningError() {
    data class ProposalInUse(val proposalId: ProposalId, val reason: String) : DeleteChangeProposalError()
    data class NetworkError(val cause: Throwable?) : DeleteChangeProposalError()
    data class PermissionDenied(val proposalId: ProposalId, val reason: String) : DeleteChangeProposalError()
    data class ProposalNotFound(val proposalId: ProposalId) : DeleteChangeProposalError()
}

/**
 * Error types for ChangeProposal exists operations.
 */
sealed class ExistsChangeProposalError : CollaborativeVersioningError() {
    data class QueryTimeout(val operation: String, val timeoutMs: Long) : ExistsChangeProposalError()
    data class NetworkError(val cause: Throwable?) : ExistsChangeProposalError()
}
