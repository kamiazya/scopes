package io.github.kamiazya.scopes.collaborativeversioning.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.collaborativeversioning.application.command.SubmitProposalCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.SubmitProposalResultDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.SubmitProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.ChangeProposalRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.SystemTimeProvider
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler

class SubmitProposalHandler(private val changeProposalRepository: ChangeProposalRepository) :
    CommandHandler<SubmitProposalCommand, SubmitProposalError, SubmitProposalResultDto> {

    override suspend fun invoke(input: SubmitProposalCommand): Either<SubmitProposalError, SubmitProposalResultDto> = either {
        val timestamp = SystemTimeProvider().now()

        val proposal = changeProposalRepository.findById(input.proposalId)
            .fold(
                { findError -> raise(SubmitProposalError.FindFailure(findError)) },
                { it },
            )

        ensureNotNull(proposal) { SubmitProposalError.ProposalNotFound(input.proposalId) }

        val submittedProposal = proposal.submit(timestamp)
            .fold(
                { domainError -> raise(SubmitProposalError.DomainRuleViolation(domainError)) },
                { it },
            )

        val savedProposal = changeProposalRepository.save(submittedProposal)
            .fold(
                { saveError -> raise(SubmitProposalError.SaveFailure(saveError)) },
                { it },
            )

        SubmitProposalResultDto.from(savedProposal)
    }
}
