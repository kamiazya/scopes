package io.github.kamiazya.scopes.collaborativeversioning.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.collaborativeversioning.application.command.ProposeChangeCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.ProposeChangeResultDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.ProposeChangeError
import io.github.kamiazya.scopes.collaborativeversioning.application.port.DomainEventPublisher
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.ProposedChange
import io.github.kamiazya.scopes.collaborativeversioning.domain.event.ProposalCreated
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.ChangeProposal
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.ChangeProposalRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.TrackedResourceRepository
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.domain.event.EventMetadata
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.github.kamiazya.scopes.platform.observability.logging.Logger
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
class ProposeChangeHandler(
    private val changeProposalRepository: ChangeProposalRepository,
    private val trackedResourceRepository: TrackedResourceRepository,
    private val eventPublisher: DomainEventPublisher,
    private val logger: Logger,
) : CommandHandler<ProposeChangeCommand, ProposeChangeError, ProposeChangeResultDto> {

    override suspend operator fun invoke(input: ProposeChangeCommand): Either<ProposeChangeError, ProposeChangeResultDto> = either {
        val timestamp = Clock.System.now()

        // Step 1: Validate target resource exists
        val targetResourceExists = trackedResourceRepository.existsById(input.targetResourceId)
            .mapLeft { repositoryError ->
                ProposeChangeError.ResourceNotFound(input.targetResourceId)
            }
            .bind()

        ensure(targetResourceExists) {
            ProposeChangeError.ResourceNotFound(input.targetResourceId)
        }

        // Step 2: Create the change proposal in DRAFT state
        val proposal = ChangeProposal.create(
            author = input.author,
            targetResourceId = input.targetResourceId,
            title = input.title,
            description = input.description,
            proposedChanges = input.proposedChanges,
            timestamp = timestamp,
        ).mapLeft { domainError ->
            ProposeChangeError.DomainRuleViolation(domainError)
        }.bind()

        // Step 3: Add any initial proposed changes if provided
        val proposalWithChanges = input.proposedChanges.fold(proposal) { currentProposal, proposedChange ->
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

        // Step 5: Publish domain event
        publishProposalCreatedEvent(savedProposal, input)

        // Step 6: Return result DTO
        ProposeChangeResultDto.from(savedProposal)
    }

    private suspend fun publishProposalCreatedEvent(proposal: ChangeProposal, input: ProposeChangeCommand) {
        val event = ProposalCreated(
            eventId = EventId.generate(),
            aggregateId = AggregateId.from(proposal.id.toString()).getOrNull() ?: AggregateId.generate(),
            aggregateVersion = AggregateVersion.from(1L).getOrNull() ?: AggregateVersion.initial(),
            occurredAt = proposal.createdAt,
            metadata = EventMetadata(
                userId = input.author.toString(),
                correlationId = input.correlationId,
            ),
            proposalId = proposal.id,
            title = proposal.title,
            description = proposal.description,
            targetScopeId = proposal.targetResourceId.toString(),
            changeType = determineChangeType(proposal.proposedChanges),
            changePayload = extractChangePayload(proposal.proposedChanges),
            createdBy = input.author.toString(),
            tags = extractTags(proposal),
        )

        eventPublisher.publish(event)
            .onLeft { error ->
                logger.warn(
                    "Failed to publish ProposalCreated event",
                    mapOf("error" to error.toString()),
                )
                // Don't fail the operation - the proposal was created successfully
            }
    }

    private fun determineChangeType(changes: List<ProposedChange>): String = when {
        changes.isEmpty() -> "empty"
        changes.size == 1 -> "single"
        else -> "composite"
    }

    private fun extractChangePayload(changes: List<ProposedChange>): Map<String, Any> = mapOf(
        "changes" to changes.map { change ->
            when (change) {
                is ProposedChange.FromChangeset -> mapOf(
                    "type" to "changeset",
                    "changesetId" to change.changesetId.toString(),
                    "description" to change.description,
                )
                is ProposedChange.Inline -> mapOf(
                    "type" to "inline",
                    "description" to change.description,
                    "changeCount" to change.changes.size,
                )
            }
        },
    )

    private fun extractTags(proposal: ChangeProposal): List<String> {
        // Extract meaningful tags from the proposal
        val tags = mutableListOf<String>()

        // Add state as tag
        tags.add(proposal.state.name.lowercase())

        // Extract hashtags from title and description
        val text = "${proposal.title} ${proposal.description}"
        tags.addAll(
            text.split(" ")
                .filter { it.startsWith("#") }
                .map { it.removePrefix("#").lowercase() },
        )

        return tags.distinct()
    }
}
