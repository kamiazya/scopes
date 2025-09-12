package io.github.kamiazya.scopes.collaborativeversioning.application.error

import io.github.kamiazya.scopes.collaborativeversioning.domain.error.ChangeProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.FindChangeProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SaveChangeProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalConflict
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalState
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId

/**
 * Application-level errors for proposal workflow operations.
 *
 * These errors provide context-specific information for each workflow operation
 * and include proper mapping from domain errors.
 */
sealed class ProposalWorkflowError(recoverable: Boolean = true) : ApplicationError(recoverable)

/**
 * Errors related to proposal creation workflow.
 */
sealed class ProposeChangeError(recoverable: Boolean = true) : ProposalWorkflowError(recoverable) {
    data class ProposalNotFound(val proposalId: ProposalId) : ProposeChangeError()

    data class ResourceNotFound(val resourceId: ResourceId) : ProposeChangeError()

    data class InvalidProposalData(val reason: String) : ProposeChangeError()

    data class DomainRuleViolation(val domainError: ChangeProposalError) : ProposeChangeError()

    data class SaveFailure(val saveError: SaveChangeProposalError) : ProposeChangeError(recoverable = false)

    data class FindFailure(val findError: FindChangeProposalError) : ProposeChangeError(recoverable = false)
}

/**
 * Errors related to proposal review workflow.
 */
sealed class ReviewProposalError(recoverable: Boolean = true) : ProposalWorkflowError(recoverable) {
    data class ProposalNotFound(val proposalId: ProposalId) : ReviewProposalError()

    data class InvalidStateForReview(val proposalId: ProposalId, val currentState: ProposalState) : ReviewProposalError()

    data class InvalidComment(val reason: String) : ReviewProposalError()

    data class CommentNotFound(val commentId: String) : ReviewProposalError()

    data class DomainRuleViolation(val domainError: ChangeProposalError) : ReviewProposalError()

    data class SaveFailure(val saveError: SaveChangeProposalError) : ReviewProposalError(recoverable = false)

    data class FindFailure(val findError: FindChangeProposalError) : ReviewProposalError(recoverable = false)
}

/**
 * Errors related to proposal approval workflow.
 */
sealed class ApproveProposalError(recoverable: Boolean = true) : ProposalWorkflowError(recoverable) {
    data class ProposalNotFound(val proposalId: ProposalId) : ApproveProposalError()

    data class InvalidStateForApproval(val proposalId: ProposalId, val currentState: ProposalState) : ApproveProposalError()

    data class HasUnresolvedIssues(val proposalId: ProposalId, val unresolvedCount: Int) : ApproveProposalError()

    data object EmptyRejectionReason : ApproveProposalError()

    data class DomainRuleViolation(val domainError: ChangeProposalError) : ApproveProposalError()

    data class SaveFailure(val saveError: SaveChangeProposalError) : ApproveProposalError(recoverable = false)

    data class FindFailure(val findError: FindChangeProposalError) : ApproveProposalError(recoverable = false)
}

/**
 * Errors related to proposal merge/application workflow.
 */
sealed class MergeProposalError(recoverable: Boolean = true) : ProposalWorkflowError(recoverable) {
    data class ProposalNotFound(val proposalId: ProposalId) : MergeProposalError()

    data class InvalidStateForMerge(val proposalId: ProposalId, val currentState: ProposalState) : MergeProposalError()

    data class ConflictsDetected(val proposalId: ProposalId, val conflicts: List<ProposalConflict>) : MergeProposalError()

    data class ResourceStateValidationFailed(val resourceId: ResourceId, val reason: String) : MergeProposalError()

    data class DomainRuleViolation(val domainError: ChangeProposalError) : MergeProposalError()

    data class SaveFailure(val saveError: SaveChangeProposalError) : MergeProposalError(recoverable = false)

    data class FindFailure(val findError: FindChangeProposalError) : MergeProposalError(recoverable = false)
}

/**
 * Errors related to proposal submission workflow.
 */
sealed class SubmitProposalError(recoverable: Boolean = true) : ProposalWorkflowError(recoverable) {
    data class ProposalNotFound(val proposalId: ProposalId) : SubmitProposalError()

    data class InvalidStateForSubmission(val proposalId: ProposalId, val currentState: ProposalState) : SubmitProposalError()

    data class NoProposedChanges(val proposalId: ProposalId) : SubmitProposalError()

    data class DomainRuleViolation(val domainError: ChangeProposalError) : SubmitProposalError()

    data class SaveFailure(val saveError: SaveChangeProposalError) : SubmitProposalError(recoverable = false)

    data class FindFailure(val findError: FindChangeProposalError) : SubmitProposalError(recoverable = false)
}
