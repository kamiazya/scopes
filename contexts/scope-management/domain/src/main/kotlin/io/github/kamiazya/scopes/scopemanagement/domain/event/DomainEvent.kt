package io.github.kamiazya.scopes.scopemanagement.domain.event

import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AggregateId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.EventId
import kotlinx.datetime.Instant

/**
 * Base class for all domain events in the system.
 *
 * Domain events capture important business occurrences and are the foundation
 * of the event-sourced architecture. Each event is immutable and represents
 * a fact that has happened in the past.
 *
 * Events must contain:
 * - The aggregate they belong to
 * - A unique event ID
 * - When the event occurred
 * - The version of the aggregate at the time
 *
 * Design principles:
 * - Events are immutable - they represent facts that have happened
 * - Events should be named in past tense (e.g., ScopeCreated, not CreateScope)
 * - Events should contain all data needed to understand what happened
 * - Events should be self-contained and not reference external state
 */
abstract class DomainEvent {
    /**
     * The ID of the aggregate this event belongs to.
     */
    abstract val aggregateId: AggregateId

    /**
     * Unique identifier for this event.
     */
    abstract val eventId: EventId

    /**
     * When this event occurred.
     */
    abstract val occurredAt: Instant

    /**
     * The version of the aggregate after this event was applied.
     */
    abstract val version: Int

    /**
     * The type name of this event for serialization/deserialization.
     */
    val eventType: String
        get() = this::class.simpleName ?: "UnknownEvent"
}
