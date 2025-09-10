package io.github.kamiazya.scopes.collaborativeversioning.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.collaborativeversioning.application.command.ProposeChangeCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.ProposeChangeResultDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.ProposeChangeError
import io.github.kamiazya.scopes.collaborativeversioning.application.port.DomainEventPublisher
import io.github.kamiazya.scopes.collaborativeversioning.domain.event.ProposalCreated
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.ChangeProposal
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.ChangeProposalRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.TrackedResourceRepository
import io.github.kamiazya.scopes.platform.domain.event.EventMetadata
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import kotlinx.datetime.Clock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

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
) {

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

        // Step 5: Publish domain event
        publishProposalCreatedEvent(savedProposal, command)
        
        // Step 6: Return result DTO
        ProposeChangeResultDto.from(savedProposal)
    }
    
    private suspend fun publishProposalCreatedEvent(
        proposal: ChangeProposal,
        command: ProposeChangeCommand,
    ) {
        val event = ProposalCreated(
            eventId = EventId.generate(),
            aggregateId = AggregateId.from(proposal.id.toString()),
            aggregateVersion = AggregateVersion.from(1L),
            occurredAt = proposal.createdAt,
            metadata = EventMetadata(
                userId = command.author,
                correlationId = command.correlationId,
            ),
            proposalId = proposal.id,
            title = proposal.title,
            description = proposal.description,
            targetScopeId = proposal.targetResourceId.toString(),
            changeType = determineChangeType(proposal.proposedChanges),
            changePayload = extractChangePayload(proposal.proposedChanges),
            createdBy = command.author,
            tags = extractTags(proposal),
        )
        
        eventPublisher.publish(event)
            .onLeft { error ->
                logger.warn { "Failed to publish ProposalCreated event: $error" }
                // Don't fail the operation - the proposal was created successfully
            }
    }
    
    private fun determineChangeType(changes: List<io.github.kamiazya.scopes.collaborativeversioning.domain.model.ProposedChange>): String =
        when {
            changes.isEmpty() -> "empty"
            changes.size == 1 -> changes.first().changeType
            else -> "composite"
        }
    
    private fun extractChangePayload(changes: List<io.github.kamiazya.scopes.collaborativeversioning.domain.model.ProposedChange>): Map<String, Any> =
        mapOf(
            "changes" to changes.map { change ->
                mapOf(
                    "type" to change.changeType,
                    "path" to change.path,
                    "operation" to change.operation,
                    "value" to change.value,
                )
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
                .map { it.removePrefix("#").lowercase() }
        )
        
        return tags.distinct()
    }
}
