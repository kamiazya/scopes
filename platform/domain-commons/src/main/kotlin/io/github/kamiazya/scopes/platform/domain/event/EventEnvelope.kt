package io.github.kamiazya.scopes.platform.domain.event

import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion

/**
 * A wrapper for domain events that tracks whether the event has been persisted with a version.
 *
 * This sealed class ensures type safety between unpersisted events (Pending) and persisted events (Persisted).
 */
sealed class EventEnvelope<out E : DomainEvent> {
    /**
     * Represents an event that has not yet been persisted to the event store.
     * The event's aggregateVersion should contain a dummy value.
     */
    data class Pending<out E : DomainEvent>(val event: E) : EventEnvelope<E>()

    /**
     * Represents an event that has been persisted to the event store with an assigned version.
     */
    data class Persisted<out E : DomainEvent>(val event: E, val version: AggregateVersion) : EventEnvelope<E>()
}
