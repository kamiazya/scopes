package io.github.kamiazya.scopes.collaborativeversioning.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.collaborativeversioning.application.command.ProposeChangeCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.ProposeChangeResultDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.ProposeChangeError
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.ChangeProposal
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.ChangeProposalRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.TrackedResourceRepository
import kotlinx.datetime.Clock

/**
 * Handler for creating new change proposals.
 *
 * This handler orchestrates the creation of a new change proposal by:
 * 1. Validating the target resource exists
 * 2. Creating the proposal in DRAFT state
 * 3. Persisting the proposal
 * 4. Returning the result DTO
 *
 * Follows the flat structure pattern with ensure()/ensureNotNull() for validation.
 */
class ProposeChangeHandler(private val changeProposalRepository: ChangeProposalRepository, private val trackedResourceRepository: TrackedResourceRepository) {

    suspend operator fun invoke(command: ProposeChangeCommand): Either<ProposeChangeError, ProposeChangeResultDto> = either {
        val timestamp = Clock.System.now()

        // Step 1: Validate target resource exists
        val targetResourceExists = trackedResourceRepository.existsById(command.targetResourceId)
            .mapLeft { repositoryError ->
                ProposeChangeError.ResourceNotFound(command.targetResourceId)
            }
            .bind()

        ensure(targetResourceExists) {
            ProposeChangeError.ResourceNotFound(command.targetResourceId)
        }

        // Step 2: Create the change proposal in DRAFT state
        val proposal = ChangeProposal.create(
            author = command.author,
            targetResourceId = command.targetResourceId,
            title = command.title,
            description = command.description,
            proposedChanges = command.proposedChanges,
            timestamp = timestamp,
        ).mapLeft { domainError ->
            ProposeChangeError.DomainRuleViolation(domainError)
        }.bind()

        // Step 3: Add any initial proposed changes if provided
        val proposalWithChanges = command.proposedChanges.fold(proposal) { currentProposal, proposedChange ->
            currentProposal.addProposedChange(proposedChange, timestamp)
                .mapLeft { domainError ->
                    ProposeChangeError.DomainRuleViolation(domainError)
                }
                .bind()
        }

        // Step 4: Persist the proposal
        val savedProposal = changeProposalRepository.save(proposalWithChanges)
            .mapLeft { saveError ->
                ProposeChangeError.SaveFailure(saveError)
            }
            .bind()

        // Step 5: Return result DTO
        ProposeChangeResultDto.from(savedProposal)
    }
}
