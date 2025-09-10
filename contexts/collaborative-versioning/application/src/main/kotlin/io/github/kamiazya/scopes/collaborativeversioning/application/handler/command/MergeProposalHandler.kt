package io.github.kamiazya.scopes.collaborativeversioning.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.collaborativeversioning.application.command.MergeProposalCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.command.SubmitProposalCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.MergeProposalResultDto
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.SubmitProposalResultDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.MergeProposalError
import io.github.kamiazya.scopes.collaborativeversioning.application.error.SubmitProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.ChangeProposalRepository
import kotlinx.datetime.Clock

/**
 * Handler for merge and submission operations on change proposals.
 *
 * This handler manages the final stages of the proposal workflow:
 * - Submit a proposal for review (DRAFT -> SUBMITTED)
 * - Merge/apply an approved proposal (APPROVED -> APPLIED)
 *
 * Follows the flat structure pattern with ensure()/ensureNotNull() for validation.
 */
class MergeProposalHandler(private val changeProposalRepository: ChangeProposalRepository) {

    suspend fun submit(command: SubmitProposalCommand): Either<SubmitProposalError, SubmitProposalResultDto> = either {
        val timestamp = Clock.System.now()

        // Step 1: Find the proposal
        val proposal = changeProposalRepository.findById(command.proposalId)
            .mapLeft { findError ->
                SubmitProposalError.FindFailure(findError)
            }
            .bind()

        ensureNotNull(proposal) {
            SubmitProposalError.ProposalNotFound(command.proposalId)
        }

        // Step 2: Submit the proposal
        val submittedProposal = proposal.submit(timestamp)
            .mapLeft { domainError ->
                SubmitProposalError.DomainRuleViolation(domainError)
            }
            .bind()

        // Step 3: Save the updated proposal
        val savedProposal = changeProposalRepository.save(submittedProposal)
            .mapLeft { saveError ->
                SubmitProposalError.SaveFailure(saveError)
            }
            .bind()

        // Step 4: Return result DTO
        SubmitProposalResultDto.from(savedProposal)
    }

    suspend fun merge(command: MergeProposalCommand): Either<MergeProposalError, MergeProposalResultDto> = either {
        val timestamp = Clock.System.now()

        // Step 1: Find the proposal
        val proposal = changeProposalRepository.findById(command.proposalId)
            .mapLeft { findError ->
                MergeProposalError.FindFailure(findError)
            }
            .bind()

        ensureNotNull(proposal) {
            MergeProposalError.ProposalNotFound(command.proposalId)
        }

        // Step 2: Detect conflicts with current resource state
        val conflicts = proposal.detectConflicts(command.currentResourceState)

        // Step 3: Check if there are any conflicts
        ensure(conflicts.isEmpty()) {
            MergeProposalError.ConflictsDetected(command.proposalId, conflicts)
        }

        // Step 4: Apply the proposal (this returns both the updated proposal and applied changes)
        val (appliedProposal, appliedChanges) = proposal.apply(timestamp)
            .mapLeft { domainError ->
                MergeProposalError.DomainRuleViolation(domainError)
            }
            .bind()

        // Step 5: Save the updated proposal
        val savedProposal = changeProposalRepository.save(appliedProposal)
            .mapLeft { saveError ->
                MergeProposalError.SaveFailure(saveError)
            }
            .bind()

        // Step 6: Return result DTO with applied changes information
        MergeProposalResultDto.from(savedProposal, appliedChanges, conflicts)
    }

    /**
     * Alternative merge operation that allows merging with conflicts.
     * This can be useful in scenarios where manual conflict resolution is handled externally.
     */
    suspend fun mergeWithConflicts(command: MergeProposalCommand): Either<MergeProposalError, MergeProposalResultDto> = either {
        val timestamp = Clock.System.now()

        // Step 1: Find the proposal
        val proposal = changeProposalRepository.findById(command.proposalId)
            .mapLeft { findError ->
                MergeProposalError.FindFailure(findError)
            }
            .bind()

        ensureNotNull(proposal) {
            MergeProposalError.ProposalNotFound(command.proposalId)
        }

        // Step 2: Detect conflicts with current resource state
        val conflicts = proposal.detectConflicts(command.currentResourceState)

        // Step 3: Apply the proposal (even with conflicts - conflicts will be reported in the result)
        val (appliedProposal, appliedChanges) = proposal.apply(timestamp)
            .mapLeft { domainError ->
                MergeProposalError.DomainRuleViolation(domainError)
            }
            .bind()

        // Step 4: Save the updated proposal
        val savedProposal = changeProposalRepository.save(appliedProposal)
            .mapLeft { saveError ->
                MergeProposalError.SaveFailure(saveError)
            }
            .bind()

        // Step 5: Return result DTO with conflicts information
        MergeProposalResultDto.from(savedProposal, appliedChanges, conflicts)
    }
}
