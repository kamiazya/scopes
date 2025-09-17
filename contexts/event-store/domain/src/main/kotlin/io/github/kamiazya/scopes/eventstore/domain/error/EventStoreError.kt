package io.github.kamiazya.scopes.eventstore.domain.error

/**
 * Base error type for event store operations.
 */
sealed class EventStoreError {

    /**
     * Error when storing an event fails.
     */
    data class StorageError(
        val aggregateId: String,
        val eventType: String,
        val eventVersion: Long? = null,
        val storageFailureType: StorageFailureType,
        val cause: Throwable? = null,
    ) : EventStoreError()

    enum class StorageFailureType {
        VERSION_CONFLICT,
        DUPLICATE_EVENT,
        SERIALIZATION_FAILED,
        VALIDATION_FAILED,
        CAPACITY_EXCEEDED,
    }

    /**
     * Error when retrieving events fails.
     */
    data class RetrievalError(
        val query: EventQuery,
        val failureReason: RetrievalFailureReason,
        val attemptedSources: List<String> = emptyList(),
        val cause: Throwable? = null,
    ) : EventStoreError()

    data class EventQuery(
        val aggregateId: String? = null,
        val eventType: String? = null,
        val fromVersion: Long? = null,
        val toVersion: Long? = null,
        val limit: Int? = null,
    )

    enum class RetrievalFailureReason {
        NOT_FOUND,
        DESERIALIZATION_FAILED,
        QUERY_TIMEOUT,
        INVALID_QUERY_PARAMETERS,
        ACCESS_DENIED,
    }

    /**
     * Error when the database connection fails.
     */
    data class DatabaseError(
        val operation: DatabaseOperation,
        val databaseType: String = "SQLite",
        val connectionState: ConnectionState,
        val cause: Throwable? = null,
    ) : EventStoreError()

    enum class DatabaseOperation {
        CONNECT,
        DISCONNECT,
        QUERY,
        INSERT,
        UPDATE,
        DELETE,
        TRANSACTION_START,
        TRANSACTION_COMMIT,
        TRANSACTION_ROLLBACK,
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        CONNECTION_LOST,
        POOL_EXHAUSTED,
    }

    /**
     * Error for invalid event data.
     */
    data class InvalidEventError(val eventType: String?, val validationErrors: List<ValidationIssue>, val eventData: Map<String, Any?> = emptyMap()) :
        EventStoreError()

    data class ValidationIssue(val field: String, val rule: ValidationRule, val actualValue: Any? = null)

    enum class ValidationRule {
        REQUIRED_FIELD_MISSING,
        INVALID_TYPE,
        INVALID_FORMAT,
        VALUE_OUT_OF_RANGE,
        UNKNOWN_EVENT_TYPE,
    }

    /**
     * Error for persistence operations.
     */
    data class PersistenceError(val operation: PersistenceOperation, val dataType: String, val dataSize: Long? = null, val retryCount: Int = 0) :
        EventStoreError()

    enum class PersistenceOperation {
        WRITE_TO_DISK,
        READ_FROM_DISK,
        CREATE_SNAPSHOT,
        DELETE_OLD_EVENTS,
        COMPACT_STORAGE,
    }
}
