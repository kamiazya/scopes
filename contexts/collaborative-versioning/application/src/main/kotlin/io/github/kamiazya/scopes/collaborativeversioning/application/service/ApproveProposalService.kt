package io.github.kamiazya.scopes.collaborativeversioning.application.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.collaborativeversioning.application.command.ApproveProposalCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.command.RejectProposalCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.ApproveProposalResultDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.ApproveProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.ChangeProposalRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.SystemTimeProvider

typealias ApproveProposalHandler = ApproveProposalService

class ApproveProposalService(private val changeProposalRepository: ChangeProposalRepository) {

    suspend fun approve(command: ApproveProposalCommand): Either<ApproveProposalError, ApproveProposalResultDto> = either {
        val timestamp = SystemTimeProvider().now()

        val proposal = changeProposalRepository.findById(command.proposalId)
            .fold(
                { findError -> raise(ApproveProposalError.FindFailure(findError)) },
                { it },
            )

        ensureNotNull(proposal) { ApproveProposalError.ProposalNotFound(command.proposalId) }

        val approvedProposal = proposal.approve(command.approver, command.approvalMessage, timestamp)
            .fold(
                { domainError -> raise(ApproveProposalError.DomainRuleViolation(domainError)) },
                { it },
            )

        val savedProposal = changeProposalRepository.save(approvedProposal)
            .fold(
                { saveError -> raise(ApproveProposalError.SaveFailure(saveError)) },
                { it },
            )

        ApproveProposalResultDto.from(savedProposal)
    }

    suspend fun reject(command: RejectProposalCommand): Either<ApproveProposalError, ApproveProposalResultDto> = either {
        val timestamp = SystemTimeProvider().now()

        val proposal = changeProposalRepository.findById(command.proposalId)
            .fold(
                { findError -> raise(ApproveProposalError.FindFailure(findError)) },
                { it },
            )

        ensureNotNull(proposal) { ApproveProposalError.ProposalNotFound(command.proposalId) }
        ensure(command.rejectionReason.isNotBlank()) { ApproveProposalError.EmptyRejectionReason() }

        val rejectedProposal = proposal.reject(command.reviewer, command.rejectionReason, timestamp)
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
