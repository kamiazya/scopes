package io.github.kamiazya.scopes.collaborativeversioning.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.collaborativeversioning.application.command.MergeProposalCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.MergeProposalResultDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.MergeProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.ChangeProposalRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.SystemTimeProvider
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler

class MergeApprovedProposalHandler(private val changeProposalRepository: ChangeProposalRepository) :
    CommandHandler<MergeProposalCommand, MergeProposalError, MergeProposalResultDto> {

    override suspend fun invoke(input: MergeProposalCommand): Either<MergeProposalError, MergeProposalResultDto> = either {
        val timestamp = SystemTimeProvider().now()

        val proposal = changeProposalRepository.findById(input.proposalId)
            .fold(
                { findError -> raise(MergeProposalError.FindFailure(findError)) },
                { it },
            )

        ensureNotNull(proposal) { MergeProposalError.ProposalNotFound(input.proposalId) }

        val conflicts = proposal.detectConflicts(input.currentResourceState)
        ensure(conflicts.isEmpty()) { MergeProposalError.ConflictsDetected(input.proposalId, conflicts) }

        val (appliedProposal, appliedChanges) = proposal.apply(timestamp)
            .fold(
                { domainError -> raise(MergeProposalError.DomainRuleViolation(domainError)) },
                { it },
            )

        val savedProposal = changeProposalRepository.save(appliedProposal)
            .fold(
                { saveError -> raise(MergeProposalError.SaveFailure(saveError)) },
                { it },
            )

        MergeProposalResultDto.from(savedProposal, appliedChanges, conflicts)
    }
}
