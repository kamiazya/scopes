package io.github.kamiazya.scopes.collaborativeversioning.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.collaborativeversioning.application.command.ApproveProposalCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.command.RejectProposalCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.ApproveProposalResultDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.ApproveProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.ChangeProposalRepository
import kotlinx.datetime.Clock

/**
 * Handler for approval-related operations on change proposals.
 *
 * This handler manages the approval workflow by providing operations to:
 * - Approve a proposal (REVIEWING -> APPROVED)
 * - Reject a proposal (REVIEWING -> REJECTED)
 *
 * Follows the flat structure pattern with ensure()/ensureNotNull() for validation.
 */
class ApproveProposalHandler(private val changeProposalRepository: ChangeProposalRepository) {

    suspend fun approve(command: ApproveProposalCommand): Either<ApproveProposalError, ApproveProposalResultDto> = either {
        val timestamp = Clock.System.now()

        // Step 1: Find the proposal
        val proposal = changeProposalRepository.findById(command.proposalId)
            .mapLeft { findError ->
                ApproveProposalError.FindFailure(findError)
            }
            .bind()

        ensureNotNull(proposal) {
            ApproveProposalError.ProposalNotFound(command.proposalId)
        }

        // Step 2: Check for unresolved issues (optional business rule)
        // Some organizations may want to prevent approval if there are unresolved issues
        val hasUnresolvedIssues = proposal.hasUnresolvedIssues()

        // For this implementation, we'll allow approval even with unresolved issues
        // but log the fact for audit purposes

        // Step 3: Approve the proposal
        val approvedProposal = proposal.approve(command.approver, command.approvalMessage, timestamp)
            .mapLeft { domainError ->
                ApproveProposalError.DomainRuleViolation(domainError)
            }
            .bind()

        // Step 4: Save the updated proposal
        val savedProposal = changeProposalRepository.save(approvedProposal)
            .mapLeft { saveError ->
                ApproveProposalError.SaveFailure(saveError)
            }
            .bind()

        // Step 5: Return result DTO
        ApproveProposalResultDto.from(savedProposal)
    }

    suspend fun reject(command: RejectProposalCommand): Either<ApproveProposalError, ApproveProposalResultDto> = either {
        val timestamp = Clock.System.now()

        // Step 1: Find the proposal
        val proposal = changeProposalRepository.findById(command.proposalId)
            .mapLeft { findError ->
                ApproveProposalError.FindFailure(findError)
            }
            .bind()

        ensureNotNull(proposal) {
            ApproveProposalError.ProposalNotFound(command.proposalId)
        }

        // Step 2: Validate rejection reason is not empty
        ensure(command.rejectionReason.isNotBlank()) {
            ApproveProposalError.EmptyRejectionReason()
        }

        // Step 3: Reject the proposal
        val rejectedProposal = proposal.reject(command.reviewer, command.rejectionReason, timestamp)
            .mapLeft { domainError ->
                ApproveProposalError.DomainRuleViolation(domainError)
            }
            .bind()

        // Step 4: Save the updated proposal
        val savedProposal = changeProposalRepository.save(rejectedProposal)
            .mapLeft { saveError ->
                ApproveProposalError.SaveFailure(saveError)
            }
            .bind()

        // Step 5: Return result DTO
        ApproveProposalResultDto.from(savedProposal)
    }
}
