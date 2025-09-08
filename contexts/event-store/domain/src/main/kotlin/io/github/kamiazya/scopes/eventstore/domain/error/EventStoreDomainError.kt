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
    data class EventOrderingViolation(val aggregateId: AggregateId, val violationType: OrderingViolationType) : EventStoreDomainError()

    enum class OrderingViolationType {
        GAPS_IN_VERSION,
        DUPLICATE_VERSION,
        FUTURE_VERSION,
        RETROACTIVE_EVENT,
    }

    /**
     * Error when attempting to store a duplicate event.
     */
    data class DuplicateEvent(val eventId: EventId) : EventStoreDomainError()

    /**
     * Error when event type is not recognized or invalid.
     */
    data class InvalidEventType(val eventType: String, val reason: InvalidReason) : EventStoreDomainError()

    enum class InvalidReason {
        NOT_REGISTERED,
        MALFORMED_TYPE_NAME,
        MISSING_HANDLER,
        DEPRECATED_TYPE,
    }

    /**
     * Error when event cannot be applied to an aggregate.
     */
    data class IncompatibleEventType(val eventType: String, val aggregateType: String) : EventStoreDomainError()
}
