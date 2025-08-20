package io.github.kamiazya.scopes.domain.aggregate

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.domain.error.AggregateConcurrencyError
import io.github.kamiazya.scopes.domain.error.ScopesError
import io.github.kamiazya.scopes.domain.event.DomainEvent
import io.github.kamiazya.scopes.domain.valueobject.AggregateId
import io.github.kamiazya.scopes.domain.valueobject.AggregateVersion
import io.github.kamiazya.scopes.domain.valueobject.EventId
import kotlinx.datetime.Instant

/**
 * Base class for all aggregate roots in the event-sourced domain model.
 *
 * This abstract class provides the fundamental infrastructure for event sourcing:
 * - Version tracking for optimistic concurrency control
 * - Uncommitted events management
 * - Event application mechanism
 * - Event replay functionality for aggregate reconstruction
 *
 * Design principles:
 * - Aggregates are the consistency boundaries
 * - All state changes must go through events
 * - Events are immutable facts about what happened
 * - Aggregates protect business invariants
 *
 * @param T The type of the aggregate implementation
 */
abstract class AggregateRoot<T : AggregateRoot<T>> {

    /**
     * The unique identifier of this aggregate.
     * This ID is immutable once set.
     */
    abstract val id: AggregateId

    /**
     * The current version of this aggregate.
     * Incremented with each event applied.
     * Used for optimistic concurrency control.
     */
    abstract val version: AggregateVersion

    /**
     * Timestamp when this aggregate was created.
     * Set from the first event's occurrence time.
     */
    abstract val createdAt: Instant

    /**
     * Timestamp of the last modification.
     * Updated with each event applied.
     */
    abstract val updatedAt: Instant

    /**
     * Collection of events that have been applied but not yet persisted.
     * These events represent the pending changes to this aggregate.
     */
    private val _uncommittedEvents = mutableListOf<DomainEvent>()

    /**
     * Gets the list of uncommitted events.
     * These are events that have been applied to the aggregate
     * but not yet persisted to the event store.
     *
     * @return An immutable list of uncommitted events
     */
    fun getUncommittedEvents(): List<DomainEvent> = _uncommittedEvents.toList()

    /**
     * Marks all events as committed by clearing the uncommitted events list.
     * This should be called after events have been successfully persisted.
     */
    fun markEventsAsCommitted() {
        _uncommittedEvents.clear()
    }

    /**
     * Applies an event to this aggregate and adds it to uncommitted events.
     * This is used when handling commands that generate new events.
     *
     * @param event The event to apply
     * @return Either an error or the updated aggregate
     */
    protected fun applyChange(event: DomainEvent): Either<ScopesError, T> = either {
        val updatedAggregate = applyEvent(event).bind()
        updatedAggregate._uncommittedEvents.add(event)
        updatedAggregate
    }

    /**
     * Abstract method to apply an event to the aggregate state.
     * Each aggregate must implement this to handle its specific events.
     * This method should be pure - no side effects allowed.
     *
     * @param event The event to apply
     * @return Either an error or the updated aggregate with the event applied
     */
    protected abstract fun applyEvent(event: DomainEvent): Either<ScopesError, T>

    /**
     * Replays a list of events to reconstruct the aggregate state.
     * Used when loading an aggregate from the event store.
     * Events are applied in order without adding them to uncommitted events.
     *
     * @param events The events to replay in chronological order
     * @return Either an error or the reconstructed aggregate
     */
    fun replay(events: List<DomainEvent>): Either<ScopesError, T> = either {
        @Suppress("UNCHECKED_CAST")
        events.fold(this@AggregateRoot as T) { aggregate, event ->
            aggregate.applyEvent(event).bind()
        }
    }

    /**
     * Validates that the expected version matches the current version.
     * Used for optimistic concurrency control to prevent lost updates.
     *
     * @param expectedVersion The version expected by the command
     * @return Either a version mismatch error or Unit on success
     */
    fun validateVersion(expectedVersion: AggregateVersion): Either<AggregateConcurrencyError, Unit> = either {
        if (version != expectedVersion) {
            raise(
                AggregateConcurrencyError.VersionMismatch(
                    aggregateId = id,
                    expectedVersion = expectedVersion.value,
                    actualVersion = version.value,
                ),
            )
        }
    }

    /**
     * Creates a new event with proper metadata.
     * Ensures consistent event creation across the aggregate.
     *
     * @param createEvent Function to create the specific event type
     * @return Either an error or the created domain event
     */
    protected inline fun <reified E : DomainEvent> newEvent(
        createEvent: (aggregateId: AggregateId, eventId: EventId, occurredAt: Instant, version: Int) -> E,
    ): Either<ScopesError, E> = either {
        val nextVersion = when (val result = version.increment()) {
            is Either.Left -> throw IllegalStateException("Version overflow: ${result.value}")
            is Either.Right -> result.value
        }

        val eventId = EventId.create(E::class).bind()

        createEvent(
            id,
            eventId,
            kotlinx.datetime.Clock.System.now(),
            nextVersion.value,
        )
    }
}
