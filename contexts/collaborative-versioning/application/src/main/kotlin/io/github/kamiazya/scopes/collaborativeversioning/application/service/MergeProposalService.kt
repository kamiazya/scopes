package io.github.kamiazya.scopes.collaborativeversioning.application.service

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
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.SystemTimeProvider

typealias MergeProposalHandler = MergeProposalService

class MergeProposalService(private val changeProposalRepository: ChangeProposalRepository) {

    suspend fun submit(command: SubmitProposalCommand): Either<SubmitProposalError, SubmitProposalResultDto> = either {
        val timestamp = SystemTimeProvider().now()

        val proposal = changeProposalRepository.findById(command.proposalId)
            .fold(
                { findError -> raise(SubmitProposalError.FindFailure(findError)) },
                { it },
            )

        ensureNotNull(proposal) { SubmitProposalError.ProposalNotFound(command.proposalId) }

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

    suspend fun merge(command: MergeProposalCommand): Either<MergeProposalError, MergeProposalResultDto> = either {
        val timestamp = SystemTimeProvider().now()

        val proposal = changeProposalRepository.findById(command.proposalId)
            .fold(
                { findError -> raise(MergeProposalError.FindFailure(findError)) },
                { it },
            )

        ensureNotNull(proposal) { MergeProposalError.ProposalNotFound(command.proposalId) }

        val conflicts = proposal.detectConflicts(command.currentResourceState)
        ensure(conflicts.isEmpty()) { MergeProposalError.ConflictsDetected(command.proposalId, conflicts) }

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

    suspend fun mergeWithConflicts(command: MergeProposalCommand): Either<MergeProposalError, MergeProposalResultDto> = either {
        val timestamp = SystemTimeProvider().now()

        val proposal = changeProposalRepository.findById(command.proposalId)
            .fold(
                { findError -> raise(MergeProposalError.FindFailure(findError)) },
                { it },
            )

        ensureNotNull(proposal) { MergeProposalError.ProposalNotFound(command.proposalId) }

        val conflicts = proposal.detectConflicts(command.currentResourceState)

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
