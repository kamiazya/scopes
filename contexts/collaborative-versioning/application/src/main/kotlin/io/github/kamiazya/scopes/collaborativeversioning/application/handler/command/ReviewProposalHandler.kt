package io.github.kamiazya.scopes.collaborativeversioning.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.collaborativeversioning.application.command.ResolveCommentCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.command.ReviewProposalCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.command.StartReviewCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.ReviewProposalResultDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.ReviewProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.ChangeProposalRepository
import kotlinx.datetime.Clock

/**
 * Handler for review-related operations on change proposals.
 *
 * This handler manages the review workflow by providing operations to:
 * - Start the review process (SUBMITTED -> REVIEWING)
 * - Add review comments during review
 * - Resolve review comments
 *
 * Follows the flat structure pattern with ensure()/ensureNotNull() for validation.
 */
class ReviewProposalHandler(private val changeProposalRepository: ChangeProposalRepository) {

    suspend fun startReview(command: StartReviewCommand): Either<ReviewProposalError, ReviewProposalResultDto> = either {
        val timestamp = Clock.System.now()

        // Step 1: Find the proposal
        val proposal = changeProposalRepository.findById(command.proposalId)
            .mapLeft { findError ->
                ReviewProposalError.FindFailure(findError)
            }
            .bind()

        ensureNotNull(proposal) {
            ReviewProposalError.ProposalNotFound(command.proposalId)
        }

        // Step 2: Start the review process
        val reviewingProposal = proposal.startReview(timestamp)
            .mapLeft { domainError ->
                ReviewProposalError.DomainRuleViolation(domainError)
            }
            .bind()

        // Step 3: Save the updated proposal
        val savedProposal = changeProposalRepository.save(reviewingProposal)
            .mapLeft { saveError ->
                ReviewProposalError.SaveFailure(saveError)
            }
            .bind()

        // Step 4: Return result DTO
        ReviewProposalResultDto.from(savedProposal)
    }

    suspend fun addComment(command: ReviewProposalCommand): Either<ReviewProposalError, ReviewProposalResultDto> = either {
        val timestamp = Clock.System.now()

        // Step 1: Find the proposal
        val proposal = changeProposalRepository.findById(command.proposalId)
            .mapLeft { findError ->
                ReviewProposalError.FindFailure(findError)
            }
            .bind()

        ensureNotNull(proposal) {
            ReviewProposalError.ProposalNotFound(command.proposalId)
        }

        // Step 2: Add the review comment
        val proposalWithComment = proposal.addReviewComment(command.comment, timestamp)
            .mapLeft { domainError ->
                ReviewProposalError.DomainRuleViolation(domainError)
            }
            .bind()

        // Step 3: Save the updated proposal
        val savedProposal = changeProposalRepository.save(proposalWithComment)
            .mapLeft { saveError ->
                ReviewProposalError.SaveFailure(saveError)
            }
            .bind()

        // Step 4: Return result DTO
        ReviewProposalResultDto.from(savedProposal)
    }

    suspend fun resolveComment(command: ResolveCommentCommand): Either<ReviewProposalError, ReviewProposalResultDto> = either {
        val timestamp = Clock.System.now()

        // Step 1: Find the proposal
        val proposal = changeProposalRepository.findById(command.proposalId)
            .mapLeft { findError ->
                ReviewProposalError.FindFailure(findError)
            }
            .bind()

        ensureNotNull(proposal) {
            ReviewProposalError.ProposalNotFound(command.proposalId)
        }

        // Step 2: Resolve the comment
        val proposalWithResolvedComment = proposal.resolveComment(command.commentId, command.resolver, timestamp)
            .mapLeft { domainError ->
                ReviewProposalError.DomainRuleViolation(domainError)
            }
            .bind()

        // Step 3: Save the updated proposal
        val savedProposal = changeProposalRepository.save(proposalWithResolvedComment)
            .mapLeft { saveError ->
                ReviewProposalError.SaveFailure(saveError)
            }
            .bind()

        // Step 4: Return result DTO
        ReviewProposalResultDto.from(savedProposal)
    }
}
