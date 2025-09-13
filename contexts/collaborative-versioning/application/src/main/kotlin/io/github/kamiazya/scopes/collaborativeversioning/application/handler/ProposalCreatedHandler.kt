package io.github.kamiazya.scopes.collaborativeversioning.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.collaborativeversioning.application.error.EventHandlingError
import io.github.kamiazya.scopes.collaborativeversioning.domain.event.ProposalCreated
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import kotlin.reflect.KClass

/**
 * Handles ProposalCreated events.
 *
 * This handler can be used to:
 * - Update read models for proposal listings
 * - Send notifications to interested parties
 * - Initialize related workflows
 */
class ProposalCreatedHandler(private val logger: Logger) : DomainEventHandler<ProposalCreated> {

    override val eventType: KClass<ProposalCreated> = ProposalCreated::class

    override suspend fun handle(event: ProposalCreated): Either<EventHandlingError, Unit> = either {
        logger.info(
            "Handling ProposalCreated event",
            mapOf(
                "proposalId" to event.proposalId.toString(),
                "title" to event.title,
                "targetScope" to event.targetScopeId,
            ),
        )

        // Here we would typically:
        // 1. Update read models (e.g., proposal list views)
        // 2. Send notifications to relevant users
        // 3. Initialize review workflows
        // 4. Update statistics or metrics

        // For now, we just log the event
        logger.debug(
            "ProposalCreated event processed successfully",
            mapOf("proposalId" to event.proposalId.toString()),
        )

        // In a real implementation, you might do something like:
        // - notificationService.notifyProposalCreated(event).bind()
        // - readModelUpdater.updateProposalList(event).bind()
        // - workflowService.initializeReviewWorkflow(event).bind()
    }
}
