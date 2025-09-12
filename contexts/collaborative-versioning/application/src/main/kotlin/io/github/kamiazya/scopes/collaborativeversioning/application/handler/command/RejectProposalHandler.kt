package io.github.kamiazya.scopes.collaborativeversioning.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.collaborativeversioning.application.command.RejectProposalCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.ApproveProposalResultDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.ApproveProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.ChangeProposalRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.SystemTimeProvider
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler

class RejectProposalHandler(private val changeProposalRepository: ChangeProposalRepository) :
    CommandHandler<RejectProposalCommand, ApproveProposalError, ApproveProposalResultDto> {

    override suspend operator fun invoke(input: RejectProposalCommand): Either<ApproveProposalError, ApproveProposalResultDto> = either {
        val timestamp = SystemTimeProvider().now()

        val proposal = changeProposalRepository.findById(input.proposalId)
            .fold(
                { findError -> raise(ApproveProposalError.FindFailure(findError)) },
                { it },
            )

        ensureNotNull(proposal) { ApproveProposalError.ProposalNotFound(input.proposalId) }

        ensure(input.rejectionReason.isNotBlank()) { ApproveProposalError.EmptyRejectionReason }

        val rejectedProposal = proposal.reject(input.reviewer, input.rejectionReason, timestamp)
            .fold(
                { domainError -> raise(ApproveProposalError.DomainRuleViolation(domainError)) },
                { it },
            )

        val savedProposal = changeProposalRepository.save(rejectedProposal)
            .fold(
                { saveError -> raise(ApproveProposalError.SaveFailure(saveError)) },
                { it },
            )

        ApproveProposalResultDto.from(savedProposal)
    }
}
