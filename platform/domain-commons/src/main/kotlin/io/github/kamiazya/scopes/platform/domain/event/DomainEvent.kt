package io.github.kamiazya.scopes.platform.domain.event

import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import kotlinx.datetime.Instant

/**
 * Base interface for all domain events in event-sourced systems.
 *
 * Domain events represent facts that have occurred in the domain.
 * They are immutable and should contain all information necessary
 * to understand what happened.
 *
 * This interface provides
 * semantic DDD marking while adding event sourcing requirements (versioning,
 * metadata) specific to this platform's needs.
 *
 * Events should be named in past tense (e.g., OrderPlaced, PaymentReceived).
 */
interface DomainEvent : org.jmolecules.event.types.DomainEvent {
    /**
     * Unique identifier for this event instance.
     */
    val eventId: EventId

    /**
     * The aggregate that this event belongs to.
     */
    val aggregateId: AggregateId

    /**
     * The version of the aggregate after this event is applied.
     * Used for ordering events and detecting gaps.
     */
    val aggregateVersion: AggregateVersion

    /**
     * When this event occurred.
     */
    val occurredAt: Instant

    /**
     * Optional metadata about the event.
     */
    val metadata: EventMetadata?
        get() = null
}

/**
 * Metadata that can be attached to domain events.
 *
 * This includes information about who caused the event,
 * correlation IDs for tracing, and other contextual data.
 */
data class EventMetadata(
    /**
     * ID of the user who caused this event.
     */
    val userId: String? = null,

    /**
     * Correlation ID for distributed tracing.
     */
    val correlationId: String? = null,

    /**
     * Causation ID - the event that caused this event.
     */
    val causationId: EventId? = null,

    /**
     * Additional custom metadata as key-value pairs.
     */
    val custom: Map<String, String> = emptyMap(),
) {
    companion object {
        val EMPTY = EventMetadata()
    }
}
