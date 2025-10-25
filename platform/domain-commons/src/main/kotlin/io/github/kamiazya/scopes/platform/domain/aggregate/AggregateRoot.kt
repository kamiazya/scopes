package io.github.kamiazya.scopes.platform.domain.aggregate

import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import org.jmolecules.ddd.types.Identifier
import org.jmolecules.ddd.types.AggregateRoot as JMoleculesAggregateRoot

/**
 * Base class for aggregate roots in Domain-Driven Design.
 *
 * Aggregate roots are the main entry points to aggregates and maintain:
 * - Identity (through any Identifier implementation)
 * - Version (for optimistic concurrency control)
 * - Uncommitted events (for event sourcing)
 *
 * This class supports both event-sourced and state-based aggregates.
 *
 * event sourcing capabilities through the abstract class.
 *
 * @param T The concrete aggregate type (for fluent API)
 * @param ID The identifier type (must implement Identifier)
 * @param E The domain event type this aggregate produces
 */
abstract class AggregateRoot<T : AggregateRoot<T, ID, E>, ID : Identifier, E : DomainEvent> : JMoleculesAggregateRoot<T, ID> {
    /**
     * Unique identifier for this aggregate instance.
     * Subclasses must provide this through getId() method implementation.
     */
    abstract override fun getId(): ID

    /**
     * Current version for optimistic concurrency control.
     */
    abstract val version: AggregateVersion

    /**
     * Events that have been raised but not yet persisted.
     * These should be cleared after successful persistence.
     */
    private val uncommittedEventsList = mutableListOf<E>()
    val uncommittedEvents: List<E> get() = uncommittedEventsList.toList()

    /**
     * Apply an event to update the aggregate state.
     *
     * This method should:
     * 1. Update the aggregate's internal state based on the event
     * 2. Return the updated aggregate (typically 'this' for mutable aggregates
     *    or a new instance for immutable aggregates)
     *
     * This method should NOT have side effects beyond state changes.
     */
    abstract fun applyEvent(event: E): T

    /**
     * Raise a new domain event.
     *
     * This method:
     * 1. Adds the event to uncommitted events
     * 2. Applies the event to update state
     * 3. Returns the updated aggregate
     */
    protected fun raiseEvent(event: E): T {
        uncommittedEventsList.add(event)
        @Suppress("UNCHECKED_CAST")
        val updated = applyEvent(event) as T
        if (updated !== this) {
            val updatedRoot = updated as AggregateRoot<T, ID, E>
            updatedRoot.uncommittedEventsList.addAll(this.uncommittedEventsList)
        }
        return updated
    }

    /**
     * Mark all uncommitted events as committed.
     * Called after successful persistence.
     */
    fun markEventsAsCommitted() {
        uncommittedEventsList.clear()
    }

    /**
     * Get and clear uncommitted events atomically.
     * Useful for event store implementations.
     */
    fun getAndClearUncommittedEvents(): List<E> {
        val events = uncommittedEventsList.toList()
        uncommittedEventsList.clear()
        return events
    }

    /**
     * Check if there are any uncommitted changes.
     */
    fun hasUncommittedChanges(): Boolean = uncommittedEventsList.isNotEmpty()

    /**
     * Get the number of uncommitted events.
     */
    fun uncommittedEventCount(): Int = uncommittedEventsList.size

    /**
     * Replay events to rebuild aggregate state.
     * Used for event sourcing from event store.
     */
    @Suppress("UNCHECKED_CAST")
    fun replayEvents(events: List<E>): T {
        var aggregate = this as T
        for (event in events) {
            aggregate = aggregate.applyEvent(event)
        }
        return aggregate
    }
}
