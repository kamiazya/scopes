package io.github.kamiazya.scopes.platform.domain.error

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Base sealed hierarchy for all domain errors in the platform layer.
 *
 * These are cross-cutting domain errors that can occur in any bounded context.
 * Each bounded context should define its own specific errors that extend
 * or wrap these platform errors as needed.
 */
sealed class DomainError {
    abstract val occurredAt: Instant
    abstract val message: String

    /**
     * Invalid aggregate ID format or value.
     */
    data class InvalidId(val value: String, val reason: String, override val occurredAt: Instant = Clock.System.now()) : DomainError() {
        override val message: String = "Invalid ID '$value': $reason"
    }

    /**
     * Invalid aggregate version.
     */
    data class InvalidVersion(val value: Long, val reason: String, override val occurredAt: Instant = Clock.System.now()) : DomainError() {
        override val message: String = "Invalid version $value: $reason"
    }

    /**
     * Invalid event ID format or value.
     */
    data class InvalidEventId(val value: String, val reason: String, override val occurredAt: Instant = Clock.System.now()) : DomainError() {
        override val message: String = "Invalid event ID '$value': $reason"
    }

    /**
     * Optimistic concurrency conflict.
     */
    data class ConcurrencyConflict(
        val aggregateId: String,
        val expectedVersion: Long,
        val actualVersion: Long,
        override val occurredAt: Instant = Clock.System.now(),
    ) : DomainError() {
        override val message: String =
            "Concurrency conflict for aggregate $aggregateId: expected version $expectedVersion but was $actualVersion"
    }

    /**
     * Aggregate not found.
     */
    data class AggregateNotFound(val aggregateId: String, val aggregateType: String? = null, override val occurredAt: Instant = Clock.System.now()) :
        DomainError() {
        override val message: String =
            "Aggregate ${aggregateType ?: ""} with ID $aggregateId not found"
    }

    /**
     * Invalid state transition.
     */
    data class InvalidStateTransition(
        val aggregateId: String,
        val currentState: String,
        val attemptedTransition: String,
        val reason: String,
        override val occurredAt: Instant = Clock.System.now(),
    ) : DomainError() {
        override val message: String =
            "Invalid state transition for aggregate $aggregateId from $currentState: $attemptedTransition - $reason"
    }
}
