package io.github.kamiazya.scopes.scopemanagement.application.error

import io.github.kamiazya.scopes.eventstore.domain.error.EventStoreError
import io.github.kamiazya.scopes.platform.application.error.BaseCrossContextErrorMapper
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError

/**
 * Cross-context error mapper for translating EventStore errors to ScopeManagement errors.
 *
 * This mapper handles the translation of errors that occur in the event-store bounded context
 * and need to be represented as errors in the scope-management bounded context.
 *
 * Key responsibilities:
 * - Map EventStore domain errors to ScopeManagement domain errors
 * - Preserve error context and semantic meaning
 * - Provide fallback mapping for unmapped errors
 * - Log unmapped errors for monitoring and debugging
 */
class EventStoreErrorMapper(logger: Logger) : BaseCrossContextErrorMapper<EventStoreError, ScopesError>(logger) {

    override fun mapCrossContext(sourceError: EventStoreError): ScopesError = when (sourceError) {
        is EventStoreError.StorageError -> {
            when (sourceError.storageFailureType) {
                EventStoreError.StorageFailureType.VERSION_CONFLICT -> ScopesError.ConcurrencyError(
                    message = "Event storage version conflict for aggregate ${sourceError.aggregateId}",
                    expectedVersion = sourceError.eventVersion?.toInt(),
                    actualVersion = null, // EventStoreError doesn't provide actual version
                    occurredAt = sourceError.occurredAt,
                )
                EventStoreError.StorageFailureType.DUPLICATE_EVENT -> ScopesError.ConcurrencyError(
                    message = "Duplicate event detected for aggregate ${sourceError.aggregateId}",
                    expectedVersion = null,
                    actualVersion = null,
                    occurredAt = sourceError.occurredAt,
                )
                EventStoreError.StorageFailureType.SERIALIZATION_FAILED -> ScopesError.SystemError(
                    message = "Event serialization failed for ${sourceError.eventType}",
                    cause = sourceError.cause,
                    errorCode = "EVENT_SERIALIZATION_FAILED",
                    context = mapOf("component" to "event-store", "operation" to "serialize-event"),
                    occurredAt = sourceError.occurredAt,
                )
                EventStoreError.StorageFailureType.VALIDATION_FAILED -> ScopesError.SystemError(
                    message = "Event validation failed for ${sourceError.eventType}",
                    cause = sourceError.cause,
                    errorCode = "EVENT_VALIDATION_FAILED",
                    context = mapOf("component" to "event-store", "operation" to "validate-event"),
                    occurredAt = sourceError.occurredAt,
                )
                EventStoreError.StorageFailureType.CAPACITY_EXCEEDED -> ScopesError.SystemError(
                    message = "Event store capacity exceeded",
                    cause = sourceError.cause,
                    errorCode = "EVENT_STORE_CAPACITY_EXCEEDED",
                    context = mapOf("component" to "event-store", "operation" to "store-event"),
                    occurredAt = sourceError.occurredAt,
                )
            }
        }

        is EventStoreError.RetrievalError -> {
            when (sourceError.failureReason) {
                EventStoreError.RetrievalFailureReason.NOT_FOUND -> ScopesError.RepositoryError(
                    message = "Events not found for query: ${sourceError.query}",
                    cause = sourceError.cause,
                    operation = "retrieve-events",
                    occurredAt = sourceError.occurredAt,
                )
                EventStoreError.RetrievalFailureReason.DESERIALIZATION_FAILED -> ScopesError.SystemError(
                    message = "Event deserialization failed",
                    cause = sourceError.cause,
                    errorCode = "EVENT_DESERIALIZATION_FAILED",
                    context = mapOf("component" to "event-store", "operation" to "deserialize-event"),
                    occurredAt = sourceError.occurredAt,
                )
                EventStoreError.RetrievalFailureReason.QUERY_TIMEOUT -> ScopesError.SystemError(
                    message = "Event query timeout",
                    cause = sourceError.cause,
                    errorCode = "EVENT_QUERY_TIMEOUT",
                    context = mapOf("component" to "event-store", "operation" to "query-events"),
                    occurredAt = sourceError.occurredAt,
                )
                EventStoreError.RetrievalFailureReason.INVALID_QUERY_PARAMETERS -> ScopesError.RepositoryError(
                    message = "Invalid event query parameters: ${sourceError.query}",
                    cause = sourceError.cause,
                    operation = "validate-query",
                    occurredAt = sourceError.occurredAt,
                )
                EventStoreError.RetrievalFailureReason.ACCESS_DENIED -> ScopesError.SystemError(
                    message = "Access denied to event store",
                    cause = sourceError.cause,
                    errorCode = "EVENT_STORE_ACCESS_DENIED",
                    context = mapOf("component" to "event-store", "operation" to "access-events"),
                    occurredAt = sourceError.occurredAt,
                )
            }
        }

        is EventStoreError.DatabaseError -> {
            val operation = when (sourceError.operation) {
                EventStoreError.DatabaseOperation.CONNECT -> "database-connect"
                EventStoreError.DatabaseOperation.DISCONNECT -> "database-disconnect"
                EventStoreError.DatabaseOperation.QUERY -> "database-query"
                EventStoreError.DatabaseOperation.INSERT -> "database-insert"
                EventStoreError.DatabaseOperation.UPDATE -> "database-update"
                EventStoreError.DatabaseOperation.DELETE -> "database-delete"
                EventStoreError.DatabaseOperation.TRANSACTION_START -> "transaction-start"
                EventStoreError.DatabaseOperation.TRANSACTION_COMMIT -> "transaction-commit"
                EventStoreError.DatabaseOperation.TRANSACTION_ROLLBACK -> "transaction-rollback"
            }

            ScopesError.SystemError(
                message = "Event store database error: ${sourceError.operation} (${sourceError.connectionState})",
                cause = sourceError.cause,
                errorCode = "EVENT_STORE_DATABASE_ERROR",
                context = mapOf("component" to "event-store-database", "operation" to operation),
                occurredAt = sourceError.occurredAt,
            )
        }

        is EventStoreError.InvalidEventError -> ScopesError.SystemError(
            message = "Invalid event data for ${sourceError.eventType}: ${sourceError.validationErrors.joinToString(", ") { "${it.field}: ${it.rule}" }}",
            cause = null,
            errorCode = "INVALID_EVENT_DATA",
            context = mapOf("component" to "event-store", "operation" to "validate-event"),
            occurredAt = sourceError.occurredAt,
        )

        is EventStoreError.PersistenceError -> {
            val operation = when (sourceError.operation) {
                EventStoreError.PersistenceOperation.WRITE_TO_DISK -> "write-events"
                EventStoreError.PersistenceOperation.READ_FROM_DISK -> "read-events"
                EventStoreError.PersistenceOperation.CREATE_SNAPSHOT -> "create-snapshot"
                EventStoreError.PersistenceOperation.DELETE_OLD_EVENTS -> "cleanup-events"
                EventStoreError.PersistenceOperation.COMPACT_STORAGE -> "compact-storage"
            }

            ScopesError.RepositoryError(
                message = "Event store persistence error during ${sourceError.operation} for ${sourceError.dataType}",
                cause = null,
                operation = operation,
                occurredAt = sourceError.occurredAt,
            )
        }

        else -> handleUnmappedCrossContextError(
            unmappedError = sourceError,
            fallbackError = ScopesError.SystemError(
                message = "Unknown event store error: ${sourceError::class.simpleName}",
                cause = null,
                errorCode = "UNKNOWN_EVENT_STORE_ERROR",
                context = mapOf("component" to "event-store", "operation" to "unknown"),
                occurredAt = sourceError.occurredAt,
            ),
        )
    }
}
