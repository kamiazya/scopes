package io.github.kamiazya.scopes.domain.event

import io.github.kamiazya.scopes.domain.valueobject.AggregateId
import io.github.kamiazya.scopes.domain.valueobject.EventId
import kotlinx.datetime.Instant

/**
 * Base class for all domain events in the system.
 *
 * Domain events capture important business occurrences and state changes.
 * They form the foundation for event sourcing and provide an audit trail
 * of all significant actions within the domain.
 *
 * Each event is immutable and represents a fact that has occurred in the past.
 */
sealed class DomainEvent {
    /**
     * URI-based identifier of the aggregate that this event relates to.
     * Format: gid://scopes/{Type}/{ID}
     * Examples:
     * - For Scope events: gid://scopes/Scope/01HX3BQXYZ
     * - For Alias events: gid://scopes/ScopeAlias/01HX3BR123
     * - For ContextView events: gid://scopes/ContextView/01HX3BS456
     */
    abstract val aggregateId: AggregateId

    /**
     * Globally unique identifier for this specific event instance.
     * Format: evt://scopes/{EventType}/{ULID}
     * Used for deduplication and event ordering.
     */
    abstract val eventId: EventId

    /**
     * The exact timestamp when this event occurred.
     * Used for temporal queries and event ordering.
     */
    abstract val occurredAt: Instant

    /**
     * Version number of the aggregate at the time of this event.
     * Used for optimistic concurrency control and event ordering within an aggregate.
     */
    abstract val version: Int

    /**
     * Type of the aggregate this event belongs to.
     * This is extracted from the aggregateId for convenience.
     * Examples: "Scope", "ScopeAlias", "ContextView"
     */
    val aggregateType: String
        get() = aggregateId.aggregateType

    /**
     * The type of event for easier filtering and handling.
     * This is extracted from the eventId for convenience.
     * Examples: "ScopeCreated", "AliasAssigned", "ContextViewUpdated"
     */
    val eventType: String
        get() = eventId.eventType
}
