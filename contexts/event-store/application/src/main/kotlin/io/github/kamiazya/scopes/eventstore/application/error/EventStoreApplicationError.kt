package io.github.kamiazya.scopes.eventstore.application.error

import io.github.kamiazya.scopes.platform.application.error.ApplicationError

/**
 * Application-level errors for Event Store operations.
 */
sealed class EventStoreApplicationError : ApplicationError {

    /**
     * Repository operation failed.
     */
    data class RepositoryError(
        val operation: RepositoryOperation,
        val aggregateId: String? = null,
        val eventType: String? = null,
        val affectedCount: Int? = null,
        override val cause: Throwable? = null,
    ) : EventStoreApplicationError()

    enum class RepositoryOperation {
        APPEND_EVENT,
        GET_EVENTS,
        GET_AGGREGATE_EVENTS,
        SAVE_SNAPSHOT,
        GET_SNAPSHOT,
        QUERY,
    }

    /**
     * Serialization or deserialization failed.
     */
    data class SerializationError(
        val operation: SerializationOperation,
        val targetType: String,
        val dataSize: Int? = null,
        val format: String = "JSON",
        override val cause: Throwable? = null,
    ) : EventStoreApplicationError()

    enum class SerializationOperation {
        SERIALIZE,
        DESERIALIZE,
    }

    /**
     * Query validation failed.
     */
    data class ValidationError(
        val parameter: String,
        val invalidValue: Any?,
        val constraint: ValidationConstraint,
        val allowedRange: String? = null,
        override val cause: Throwable? = null,
    ) : EventStoreApplicationError()

    enum class ValidationConstraint {
        NEGATIVE_LIMIT,
        ZERO_LIMIT,
        FUTURE_TIMESTAMP,
        INVALID_DATE_RANGE,
        INVALID_AGGREGATE_ID,
        INVALID_EVENT_TYPE,
    }
}
