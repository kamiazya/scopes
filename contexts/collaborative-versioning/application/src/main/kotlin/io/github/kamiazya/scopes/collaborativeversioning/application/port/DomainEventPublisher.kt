package io.github.kamiazya.scopes.collaborativeversioning.application.port

import arrow.core.Either
import io.github.kamiazya.scopes.collaborativeversioning.application.error.EventPublishingError
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent

/**
 * Port for publishing domain events from the collaborative versioning context.
 *
 * This interface abstracts the event publishing mechanism, allowing the
 * application layer to emit events without depending on the infrastructure.
 */
interface DomainEventPublisher {

    /**
     * Publish a domain event.
     *
     * @param event The domain event to publish
     * @return Either an error if publishing failed, or Unit if successful
     */
    suspend fun publish(event: DomainEvent): Either<EventPublishingError, Unit>

    /**
     * Publish multiple domain events in order.
     *
     * Events are published sequentially to maintain ordering.
     * If any event fails to publish, the operation stops and returns an error.
     *
     * @param events The domain events to publish
     * @return Either an error if any publishing failed, or Unit if all successful
     */
    suspend fun publishAll(events: List<DomainEvent>): Either<EventPublishingError, Unit>
}
