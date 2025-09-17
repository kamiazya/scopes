package io.github.kamiazya.scopes.platform.domain.error

/**
 * Base sealed hierarchy for all domain errors in the platform layer.
 *
 * These are cross-cutting domain errors that can occur in any bounded context.
 * Each bounded context should define its own specific errors that extend
 * or wrap these platform errors as needed.
 */
sealed class DomainError {

    /**
     * Invalid aggregate ID format or value.
     */
    data class InvalidId(val value: String, val errorType: InvalidIdType) : DomainError() {
        enum class InvalidIdType {
            EMPTY,
            INVALID_FORMAT,
            INVALID_CHARACTERS,
            TOO_LONG,
            TOO_SHORT,
        }
    }

    /**
     * Invalid aggregate version.
     */
    data class InvalidVersion(val value: Long, val errorType: InvalidVersionType) : DomainError() {
        enum class InvalidVersionType {
            NEGATIVE,
            TOO_LARGE,
            INVALID_INCREMENT,
        }
    }

    /**
     * Invalid event ID format or value.
     */
    data class InvalidEventId(val value: String, val errorType: InvalidEventIdType) : DomainError() {
        enum class InvalidEventIdType {
            EMPTY,
            INVALID_FORMAT,
            INVALID_UUID,
        }
    }

    /**
     * Optimistic concurrency conflict.
     */
    data class ConcurrencyConflict(val aggregateId: String, val expectedVersion: Long, val actualVersion: Long) : DomainError()

    /**
     * Aggregate not found.
     */
    data class AggregateNotFound(val aggregateId: String, val aggregateType: String? = null) : DomainError()

    /**
     * Invalid state transition.
     */
    data class InvalidStateTransition(
        val aggregateId: String,
        val currentState: String,
        val attemptedTransition: String,
        val errorType: InvalidStateTransitionType,
    ) : DomainError() {
        enum class InvalidStateTransitionType {
            INVALID_SOURCE_STATE,
            INVALID_TARGET_STATE,
            TRANSITION_NOT_ALLOWED,
            PRECONDITION_NOT_MET,
        }
    }
}
