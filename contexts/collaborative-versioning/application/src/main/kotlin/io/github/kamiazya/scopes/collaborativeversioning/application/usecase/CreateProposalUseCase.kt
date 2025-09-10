package io.github.kamiazya.scopes.collaborativeversioning.application.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.collaborativeversioning.application.port.DomainEventPublisher
import io.github.kamiazya.scopes.collaborativeversioning.domain.event.ProposalCreated
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId
import io.github.kamiazya.scopes.platform.domain.event.EventMetadata
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import kotlinx.datetime.Clock

/**
 * Use case for creating change proposals.
 *
 * This use case handles the creation of new proposals and publishes
 * the appropriate domain events.
 */
class CreateProposalUseCase(
    private val eventPublisher: DomainEventPublisher,
    // Other dependencies like repositories would go here
) {

    suspend fun execute(
        title: String,
        description: String,
        targetScopeId: String,
        changeType: String,
        changePayload: Map<String, Any>,
        userId: String,
    ): Either<CreateProposalError, ProposalId> = either {
        // 1. Validate inputs (simplified for example)
        ensure(title.isNotBlank()) { CreateProposalError.InvalidTitle }
        ensure(description.isNotBlank()) { CreateProposalError.InvalidDescription }

        // 2. Create the proposal (normally would involve repository operations)
        val proposalId = ProposalId.generate()
        val aggregateId = AggregateId.from(proposalId.toString()).getOrNull() ?: AggregateId.generate()

        // In real implementation:
        // - Create proposal entity
        // - Save to repository
        // - Check business rules

        // 3. Create the domain event
        val event = ProposalCreated(
            eventId = EventId.generate(),
            aggregateId = aggregateId,
            aggregateVersion = AggregateVersion.from(1L).getOrNull() ?: AggregateVersion.initial(), // First version
            occurredAt = Clock.System.now(),
            metadata = EventMetadata(
                userId = userId,
                correlationId = generateCorrelationId(),
            ),
            proposalId = proposalId,
            title = title,
            description = description,
            targetScopeId = targetScopeId,
            changeType = changeType,
            changePayload = changePayload,
            createdBy = userId,
            tags = extractTags(title, description),
        )

        // 4. Publish the event
        eventPublisher.publish(event)
            .mapLeft { publishError ->
                // Log the error but don't fail the operation
                // Events can be republished from event sourcing if needed
                logEventPublishingError(publishError)
                // Continue with success - the proposal was created
            }

        // 5. Return the result
        proposalId
    }

    private fun generateCorrelationId(): String = "proposal-${System.currentTimeMillis()}"

    private fun extractTags(title: String, description: String): List<String> {
        // Example: extract hashtags or keywords
        val text = "$title $description"
        return text.split(" ")
            .filter { it.startsWith("#") }
            .map { it.removePrefix("#") }
            .distinct()
    }

    private fun logEventPublishingError(error: Any) {
        // In real implementation, use proper logging
        println("Warning: Failed to publish event: $error")
    }
}

/**
 * Example errors for the use case.
 */
sealed class CreateProposalError {
    object InvalidTitle : CreateProposalError()
    object InvalidDescription : CreateProposalError()
    object TargetScopeNotFound : CreateProposalError()
    object Unauthorized : CreateProposalError()
}

/**
 * Use case for approving change proposals.
 */
class ApproveProposalUseCase(private val eventPublisher: DomainEventPublisher) {

    suspend fun execute(proposalId: ProposalId, approverId: String, comment: String?): Either<ApproveProposalError, Unit> = either {
        // In a real implementation:
        // 1. Load proposal from repository
        // 2. Validate it can be approved
        // 3. Update proposal state
        // 4. Save changes

        // 5. Publish multiple related events
        val events = listOf(
            createProposalReviewedEvent(proposalId, approverId, comment),
            createProposalApprovedEvent(proposalId, approverId, comment),
        )

        // Publish all events in order
        eventPublisher.publishAll(events)
            .mapLeft { publishError ->
                // Log but don't fail the approval
                logEventPublishingError(publishError)
            }
    }

    private fun createProposalReviewedEvent(
        proposalId: ProposalId,
        reviewerId: String,
        comment: String?,
    ): io.github.kamiazya.scopes.collaborativeversioning.domain.event.ProposalReviewed {
        // Implementation would create the actual event
        TODO("Create ProposalReviewed event")
    }

    private fun createProposalApprovedEvent(
        proposalId: ProposalId,
        approverId: String,
        comment: String?,
    ): io.github.kamiazya.scopes.collaborativeversioning.domain.event.ProposalApproved {
        // Implementation would create the actual event
        TODO("Create ProposalApproved event")
    }

    private fun logEventPublishingError(error: Any) {
        println("Warning: Failed to publish events: $error")
    }
}

sealed class ApproveProposalError {
    object ProposalNotFound : ApproveProposalError()
    object InvalidState : ApproveProposalError()
    object Unauthorized : ApproveProposalError()
}
