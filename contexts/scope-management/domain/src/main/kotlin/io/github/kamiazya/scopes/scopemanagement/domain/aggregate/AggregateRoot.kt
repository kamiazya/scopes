package io.github.kamiazya.scopes.scopemanagement.domain.aggregate

import io.github.kamiazya.scopes.scopemanagement.domain.event.DomainEvent
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AggregateId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AggregateVersion

/**
 * Base class for aggregate roots in event sourced systems.
 *
 * Aggregate roots are the main entry points to aggregates and maintain:
 * - Identity (through AggregateId)
 * - Version (for optimistic concurrency control)
 * - Uncommitted events (events raised but not yet persisted)
 *
 * @param T The concrete aggregate type
 */
abstract class AggregateRoot<T : AggregateRoot<T>> {
    abstract val id: AggregateId
    abstract val version: AggregateVersion

    private val _uncommittedEvents = mutableListOf<DomainEvent>()
    val uncommittedEvents: List<DomainEvent> get() = _uncommittedEvents.toList()

    /**
     * Apply an event to update the aggregate state.
     * This method should be overridden in concrete aggregates to handle specific events.
     */
    abstract fun applyEvent(event: DomainEvent): T

    /**
     * Raise a new domain event.
     * The event is added to the uncommitted events list and applied to update state.
     */
    protected fun raiseEvent(event: DomainEvent): T {
        _uncommittedEvents.add(event)
        @Suppress("UNCHECKED_CAST")
        return applyEvent(event) as T
    }

    /**
     * Mark all events as committed (typically after successful persistence).
     */
    fun markEventsAsCommitted() {
        _uncommittedEvents.clear()
    }

    /**
     * Load aggregate from a history of events.
     */
    fun loadFromHistory(events: List<DomainEvent>): T {
        var aggregate = this
        events.forEach { event ->
            aggregate = aggregate.applyEvent(event)
        }
        @Suppress("UNCHECKED_CAST")
        return aggregate as T
    }
}
