package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

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
                    aggregateId = sourceError.aggregateId,
                    aggregateType = "Scope",
                    expectedVersion = sourceError.eventVersion?.toInt(),
                    actualVersion = null, // EventStoreError doesn't provide actual version
                    operation = "save_events",
                    occurredAt = sourceError.occurredAt,
                )
                EventStoreError.StorageFailureType.DUPLICATE_EVENT -> ScopesError.ConcurrencyError(
                    aggregateId = sourceError.aggregateId,
                    aggregateType = "Scope",
                    expectedVersion = null,
                    actualVersion = null,
                    operation = "save_events",
                    occurredAt = sourceError.occurredAt,
                )
                EventStoreError.StorageFailureType.SERIALIZATION_FAILED -> ScopesError.SystemError(
                    errorType = ScopesError.SystemError.SystemErrorType.SERIALIZATION_FAILED,
                    service = "event-store",
                    cause = sourceError.cause,
                    context = mapOf("component" to "event-store", "operation" to "serialize-event", "eventType" to sourceError.eventType),
                    occurredAt = sourceError.occurredAt,
                )
                EventStoreError.StorageFailureType.VALIDATION_FAILED -> ScopesError.ValidationFailed(
                    field = "event",
                    value = sourceError.eventType,
                    constraint = ScopesError.ValidationConstraintType.InvalidValue("Event validation failed"),
                    details = mapOf("component" to "event-store", "operation" to "validate-event"),
                    occurredAt = sourceError.occurredAt,
                )
                EventStoreError.StorageFailureType.CAPACITY_EXCEEDED -> ScopesError.SystemError(
                    errorType = ScopesError.SystemError.SystemErrorType.CAPACITY_EXCEEDED,
                    service = "event-store",
                    cause = sourceError.cause,
                    context = mapOf("component" to "event-store", "operation" to "store-event"),
                    occurredAt = sourceError.occurredAt,
                )
            }
        }

        is EventStoreError.RetrievalError -> {
            when (sourceError.failureReason) {
                EventStoreError.RetrievalFailureReason.NOT_FOUND -> ScopesError.RepositoryError(
                    repositoryName = "EventStore",
                    operation = ScopesError.RepositoryError.RepositoryOperation.QUERY,
                    entityType = "Event",
                    cause = sourceError.cause,
                    occurredAt = sourceError.occurredAt,
                )
                EventStoreError.RetrievalFailureReason.DESERIALIZATION_FAILED -> ScopesError.SystemError(
                    errorType = ScopesError.SystemError.SystemErrorType.DESERIALIZATION_FAILED,
                    service = "event-store",
                    cause = sourceError.cause,
                    context = mapOf("component" to "event-store", "operation" to "deserialize-event"),
                    occurredAt = sourceError.occurredAt,
                )
                EventStoreError.RetrievalFailureReason.QUERY_TIMEOUT -> ScopesError.SystemError(
                    errorType = ScopesError.SystemError.SystemErrorType.QUERY_TIMEOUT,
                    service = "event-store",
                    cause = sourceError.cause,
                    context = mapOf("component" to "event-store", "operation" to "query-events"),
                    occurredAt = sourceError.occurredAt,
                )
                EventStoreError.RetrievalFailureReason.INVALID_QUERY_PARAMETERS -> ScopesError.RepositoryError(
                    repositoryName = "EventStore",
                    operation = ScopesError.RepositoryError.RepositoryOperation.QUERY,
                    entityType = "Event",
                    cause = sourceError.cause,
                    occurredAt = sourceError.occurredAt,
                )
                EventStoreError.RetrievalFailureReason.ACCESS_DENIED -> ScopesError.SystemError(
                    errorType = ScopesError.SystemError.SystemErrorType.ACCESS_DENIED,
                    service = "event-store",
                    cause = sourceError.cause,
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
                errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                service = "event-store-database",
                cause = sourceError.cause,
                context = mapOf(
                    "component" to "event-store-database",
                    "operation" to operation,
                    "databaseOperation" to sourceError.operation.name,
                    "connectionState" to sourceError.connectionState.name,
                ),
                occurredAt = sourceError.occurredAt,
            )
        }

        is EventStoreError.InvalidEventError -> ScopesError.ValidationFailed(
            field = "event",
            value = sourceError.eventType ?: "",
            constraint = ScopesError.ValidationConstraintType.InvalidValue(
                sourceError.validationErrors.joinToString(", ") { "${it.field}: ${it.rule}" },
            ),
            details = mapOf(
                "component" to "event-store",
                "operation" to "validate-event",
                "validationErrors" to sourceError.validationErrors,
            ),
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
                repositoryName = "EventStore",
                operation = when (sourceError.operation) {
                    EventStoreError.PersistenceOperation.WRITE_TO_DISK -> ScopesError.RepositoryError.RepositoryOperation.SAVE
                    EventStoreError.PersistenceOperation.READ_FROM_DISK -> ScopesError.RepositoryError.RepositoryOperation.FIND
                    EventStoreError.PersistenceOperation.CREATE_SNAPSHOT -> ScopesError.RepositoryError.RepositoryOperation.SAVE
                    EventStoreError.PersistenceOperation.DELETE_OLD_EVENTS -> ScopesError.RepositoryError.RepositoryOperation.DELETE
                    EventStoreError.PersistenceOperation.COMPACT_STORAGE -> ScopesError.RepositoryError.RepositoryOperation.UPDATE
                },
                entityType = sourceError.dataType,
                cause = null,
                occurredAt = sourceError.occurredAt,
            )
        }

        else -> handleUnmappedCrossContextError(
            unmappedError = sourceError,
            fallbackError = ScopesError.SystemError(
                errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                service = "event-store",
                cause = null,
                context = mapOf("component" to "event-store", "operation" to "unspecified"),
                occurredAt = sourceError.occurredAt,
            ),
        )
    }
}
