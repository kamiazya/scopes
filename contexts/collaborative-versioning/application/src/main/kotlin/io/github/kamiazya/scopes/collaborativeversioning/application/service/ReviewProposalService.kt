package io.github.kamiazya.scopes.collaborativeversioning.application.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.collaborativeversioning.application.command.ResolveCommentCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.command.ReviewProposalCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.command.StartReviewCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.ReviewProposalResultDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.ReviewProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.ChangeProposalRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.SystemTimeProvider

typealias ReviewProposalHandler = ReviewProposalService

class ReviewProposalService(private val changeProposalRepository: ChangeProposalRepository) {

    suspend fun startReview(command: StartReviewCommand): Either<ReviewProposalError, ReviewProposalResultDto> = either {
        val timestamp = SystemTimeProvider().now()

        val proposal = changeProposalRepository.findById(command.proposalId)
            .fold(
                { findError -> raise(ReviewProposalError.FindFailure(findError)) },
                { it },
            )

        ensureNotNull(proposal) { ReviewProposalError.ProposalNotFound(command.proposalId) }

        val reviewingProposal = proposal.startReview(timestamp)
            .fold(
                { domainError -> raise(ReviewProposalError.DomainRuleViolation(domainError)) },
                { it },
            )

        val savedProposal = changeProposalRepository.save(reviewingProposal)
            .fold(
                { saveError -> raise(ReviewProposalError.SaveFailure(saveError)) },
                { it },
            )

        ReviewProposalResultDto.from(savedProposal)
    }

    suspend fun addComment(command: ReviewProposalCommand): Either<ReviewProposalError, ReviewProposalResultDto> = either {
        val timestamp = SystemTimeProvider().now()

        val proposal = changeProposalRepository.findById(command.proposalId)
            .fold(
                { findError -> raise(ReviewProposalError.FindFailure(findError)) },
                { it },
            )

        ensureNotNull(proposal) { ReviewProposalError.ProposalNotFound(command.proposalId) }

        val proposalWithComment = proposal.addReviewComment(command.comment, timestamp)
            .fold(
                { domainError -> raise(ReviewProposalError.DomainRuleViolation(domainError)) },
                { it },
            )

        val savedProposal = changeProposalRepository.save(proposalWithComment)
            .fold(
                { saveError -> raise(ReviewProposalError.SaveFailure(saveError)) },
                { it },
            )

        ReviewProposalResultDto.from(savedProposal)
    }

    suspend fun resolveComment(command: ResolveCommentCommand): Either<ReviewProposalError, ReviewProposalResultDto> = either {
        val timestamp = SystemTimeProvider().now()

        val proposal = changeProposalRepository.findById(command.proposalId)
            .fold(
                { findError -> raise(ReviewProposalError.FindFailure(findError)) },
                { it },
            )

        ensureNotNull(proposal) { ReviewProposalError.ProposalNotFound(command.proposalId) }

        val proposalWithResolvedComment = proposal.resolveComment(command.commentId, command.resolver, timestamp)
            .fold(
                { domainError -> raise(ReviewProposalError.DomainRuleViolation(domainError)) },
                { it },
            )

        val savedProposal = changeProposalRepository.save(proposalWithResolvedComment)
            .fold(
                { saveError -> raise(ReviewProposalError.SaveFailure(saveError)) },
                { it },
            )

        ReviewProposalResultDto.from(savedProposal)
    }
}
