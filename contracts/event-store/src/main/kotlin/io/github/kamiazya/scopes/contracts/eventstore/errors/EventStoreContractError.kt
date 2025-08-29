package io.github.kamiazya.scopes.contracts.eventstore.errors

import kotlinx.datetime.Instant

/**
 * Base error type for event store contract operations.
 */
public sealed class EventStoreContractError {
    /**
     * Error when an event cannot be stored.
     */
    public data class EventStorageError(
        public val aggregateId: String,
        public val eventType: String,
        public val eventVersion: Long? = null,
        public val storageReason: StorageFailureReason,
        public val conflictingVersion: Long? = null,
        public val occurredAt: Instant,
        public val cause: Throwable? = null,
    ) : EventStoreContractError()

    public enum class StorageFailureReason {
        VERSION_CONFLICT,
        DUPLICATE_EVENT,
        STORAGE_FULL,
        WRITE_TIMEOUT,
        INVALID_EVENT,
        ACCESS_DENIED,
    }

    /**
     * Error when events cannot be retrieved.
     */
    public data class EventRetrievalError(
        public val aggregateId: String? = null,
        public val eventType: String? = null,
        public val timeRange: TimeRange? = null,
        public val queryLimit: Int? = null,
        public val retrievalReason: RetrievalFailureReason,
        public val occurredAt: Instant,
        public val cause: Throwable? = null,
    ) : EventStoreContractError()

    public data class TimeRange(public val start: Instant, public val end: Instant)

    public enum class RetrievalFailureReason {
        NOT_FOUND,
        ACCESS_DENIED,
        CORRUPTED_DATA,
        TIMEOUT,
        INVALID_QUERY,
    }

    /**
     * Error for invalid query parameters.
     */
    public data class InvalidQueryError(
        public val parameterName: String,
        public val providedValue: Any?,
        public val constraint: QueryConstraint,
        public val expectedFormat: String? = null,
        public val occurredAt: Instant,
    ) : EventStoreContractError()

    public enum class QueryConstraint {
        MISSING_REQUIRED,
        INVALID_FORMAT,
        OUT_OF_RANGE,
        INVALID_COMBINATION,
        UNSUPPORTED_VALUE,
    }
}
