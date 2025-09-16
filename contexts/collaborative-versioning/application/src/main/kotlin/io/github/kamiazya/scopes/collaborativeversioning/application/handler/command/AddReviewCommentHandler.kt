package io.github.kamiazya.scopes.collaborativeversioning.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.collaborativeversioning.application.command.ReviewProposalCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.ReviewProposalResultDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.ReviewProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.ChangeProposalRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.SystemTimeProvider
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler

class AddReviewCommentHandler(private val changeProposalRepository: ChangeProposalRepository) :
    CommandHandler<ReviewProposalCommand, ReviewProposalError, ReviewProposalResultDto> {

    override suspend fun invoke(input: ReviewProposalCommand): Either<ReviewProposalError, ReviewProposalResultDto> = either {
        val timestamp = SystemTimeProvider().now()

        val proposal = changeProposalRepository.findById(input.proposalId)
            .fold(
                { findError -> raise(ReviewProposalError.FindFailure(findError)) },
                { it },
            )

        ensureNotNull(proposal) { ReviewProposalError.ProposalNotFound(input.proposalId) }

        val proposalWithComment = proposal.addReviewComment(input.comment, timestamp)
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
}
