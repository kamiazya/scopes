package io.github.kamiazya.scopes.collaborativeversioning.application.error

import io.github.kamiazya.scopes.collaborativeversioning.domain.error.ChangeProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.FindChangeProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SaveChangeProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.Conflict
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalState
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Application-level errors for proposal workflow operations.
 *
 * These errors provide context-specific information for each workflow operation
 * and include proper mapping from domain errors.
 */
sealed class ProposalWorkflowError {
    abstract val occurredAt: Instant
}

/**
 * Errors related to proposal creation workflow.
 */
sealed class ProposeChangeError : ProposalWorkflowError() {
    data class ProposalNotFound(val proposalId: ProposalId, override val occurredAt: Instant = Clock.System.now()) : ProposeChangeError()

    data class ResourceNotFound(val resourceId: ResourceId, override val occurredAt: Instant = Clock.System.now()) : ProposeChangeError()

    data class InvalidProposalData(val reason: String, override val occurredAt: Instant = Clock.System.now()) : ProposeChangeError()

    data class DomainRuleViolation(val domainError: ChangeProposalError, override val occurredAt: Instant = Clock.System.now()) : ProposeChangeError()

    data class SaveFailure(val saveError: SaveChangeProposalError, override val occurredAt: Instant = Clock.System.now()) : ProposeChangeError()

    data class FindFailure(val findError: FindChangeProposalError, override val occurredAt: Instant = Clock.System.now()) : ProposeChangeError()
}

/**
 * Errors related to proposal review workflow.
 */
sealed class ReviewProposalError : ProposalWorkflowError() {
    data class ProposalNotFound(val proposalId: ProposalId, override val occurredAt: Instant = Clock.System.now()) : ReviewProposalError()

    data class InvalidStateForReview(val proposalId: ProposalId, val currentState: ProposalState, override val occurredAt: Instant = Clock.System.now()) :
        ReviewProposalError()

    data class InvalidComment(val reason: String, override val occurredAt: Instant = Clock.System.now()) : ReviewProposalError()

    data class CommentNotFound(val commentId: String, override val occurredAt: Instant = Clock.System.now()) : ReviewProposalError()

    data class DomainRuleViolation(val domainError: ChangeProposalError, override val occurredAt: Instant = Clock.System.now()) : ReviewProposalError()

    data class SaveFailure(val saveError: SaveChangeProposalError, override val occurredAt: Instant = Clock.System.now()) : ReviewProposalError()

    data class FindFailure(val findError: FindChangeProposalError, override val occurredAt: Instant = Clock.System.now()) : ReviewProposalError()
}

/**
 * Errors related to proposal approval workflow.
 */
sealed class ApproveProposalError : ProposalWorkflowError() {
    data class ProposalNotFound(val proposalId: ProposalId, override val occurredAt: Instant = Clock.System.now()) : ApproveProposalError()

    data class InvalidStateForApproval(val proposalId: ProposalId, val currentState: ProposalState, override val occurredAt: Instant = Clock.System.now()) :
        ApproveProposalError()

    data class HasUnresolvedIssues(val proposalId: ProposalId, val unresolvedCount: Int, override val occurredAt: Instant = Clock.System.now()) :
        ApproveProposalError()

    data class EmptyRejectionReason(override val occurredAt: Instant = Clock.System.now()) : ApproveProposalError()

    data class DomainRuleViolation(val domainError: ChangeProposalError, override val occurredAt: Instant = Clock.System.now()) : ApproveProposalError()

    data class SaveFailure(val saveError: SaveChangeProposalError, override val occurredAt: Instant = Clock.System.now()) : ApproveProposalError()

    data class FindFailure(val findError: FindChangeProposalError, override val occurredAt: Instant = Clock.System.now()) : ApproveProposalError()
}

/**
 * Errors related to proposal merge/application workflow.
 */
sealed class MergeProposalError : ProposalWorkflowError() {
    data class ProposalNotFound(val proposalId: ProposalId, override val occurredAt: Instant = Clock.System.now()) : MergeProposalError()

    data class InvalidStateForMerge(val proposalId: ProposalId, val currentState: ProposalState, override val occurredAt: Instant = Clock.System.now()) :
        MergeProposalError()

    data class ConflictsDetected(val proposalId: ProposalId, val conflicts: List<Conflict>, override val occurredAt: Instant = Clock.System.now()) :
        MergeProposalError()

    data class ResourceStateValidationFailed(val resourceId: ResourceId, val reason: String, override val occurredAt: Instant = Clock.System.now()) :
        MergeProposalError()

    data class DomainRuleViolation(val domainError: ChangeProposalError, override val occurredAt: Instant = Clock.System.now()) : MergeProposalError()

    data class SaveFailure(val saveError: SaveChangeProposalError, override val occurredAt: Instant = Clock.System.now()) : MergeProposalError()

    data class FindFailure(val findError: FindChangeProposalError, override val occurredAt: Instant = Clock.System.now()) : MergeProposalError()
}

/**
 * Errors related to proposal submission workflow.
 */
sealed class SubmitProposalError : ProposalWorkflowError() {
    data class ProposalNotFound(val proposalId: ProposalId, override val occurredAt: Instant = Clock.System.now()) : SubmitProposalError()

    data class InvalidStateForSubmission(val proposalId: ProposalId, val currentState: ProposalState, override val occurredAt: Instant = Clock.System.now()) :
        SubmitProposalError()

    data class NoProposedChanges(val proposalId: ProposalId, override val occurredAt: Instant = Clock.System.now()) : SubmitProposalError()

    data class DomainRuleViolation(val domainError: ChangeProposalError, override val occurredAt: Instant = Clock.System.now()) : SubmitProposalError()

    data class SaveFailure(val saveError: SaveChangeProposalError, override val occurredAt: Instant = Clock.System.now()) : SubmitProposalError()

    data class FindFailure(val findError: FindChangeProposalError, override val occurredAt: Instant = Clock.System.now()) : SubmitProposalError()
}
