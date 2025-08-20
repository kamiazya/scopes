package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.event.DomainEvent

/**
 * Interface for publishing domain events to other parts of the system.
 *
 * This interface represents the outbound port for event publication
 * in the hexagonal architecture. It allows the domain layer to publish
 * events without knowing about the specific infrastructure used for
 * event distribution (e.g., message queues, event streams, etc.).
 *
 * Implementations might include:
 * - In-memory event bus for local event handling
 * - Message queue integration (RabbitMQ, Kafka, etc.)
 * - Event store integration
 * - WebSocket broadcasting for real-time updates
 *
 * Design principles:
 * - Events should be published in the order they occurred
 * - Publication should be reliable but non-blocking
 * - Failed publications should not affect domain operations
 * - Events are immutable once published
 */
interface DomainEventPublisher {
    /**
     * Publishes a single event.
     *
     * @param event The domain event to publish
     */
    suspend fun publish(event: DomainEvent)

    /**
     * Publishes multiple events in order.
     *
     * Events should be published in the exact order provided to maintain
     * causal relationships between events.
     *
     * @param events The list of events to publish in order
     */
    suspend fun publishAll(events: List<DomainEvent>) {
        events.forEach { publish(it) }
    }
}
