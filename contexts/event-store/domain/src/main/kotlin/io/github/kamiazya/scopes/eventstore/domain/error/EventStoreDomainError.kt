package io.github.kamiazya.scopes.eventstore.domain.error

import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.EventId

/**
 * Domain errors for the event store bounded context.
 */
sealed class EventStoreDomainError {

    /**
     * Error when aggregate version doesn't match expected version.
     */
    data class InvalidAggregateVersion(val aggregateId: AggregateId, val expectedVersion: Long, val actualVersion: Long) : EventStoreDomainError()

    /**
     * Error when event ordering rules are violated.
     */
    data class EventOrderingViolation(val aggregateId: AggregateId, val message: String) : EventStoreDomainError()

    /**
     * Error when attempting to store a duplicate event.
     */
    data class DuplicateEvent(val eventId: EventId) : EventStoreDomainError()

    /**
     * Error when event type is not recognized or invalid.
     */
    data class InvalidEventType(val eventType: String, val message: String) : EventStoreDomainError()

    /**
     * Error when event cannot be applied to an aggregate.
     */
    data class IncompatibleEventType(val eventType: String, val aggregateType: String) : EventStoreDomainError()
}
