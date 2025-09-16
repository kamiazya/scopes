package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import io.github.kamiazya.scopes.contracts.eventstore.errors.EventStoreContractError
import io.github.kamiazya.scopes.platform.application.error.BaseCrossContextErrorMapper
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError

/**
 * Maps EventStore contract errors to ScopeManagement domain errors.
 *
 * This mapper handles translation between the public contract errors
 * and internal domain errors, ensuring proper error context preservation.
 */
internal class EventStoreContractErrorMapper(logger: Logger) : BaseCrossContextErrorMapper<EventStoreContractError, ScopesError>(logger) {

    override fun mapCrossContext(sourceError: EventStoreContractError): ScopesError = when (sourceError) {
        is EventStoreContractError.EventStorageError -> when (sourceError.storageReason) {
            EventStoreContractError.StorageFailureReason.VERSION_CONFLICT -> ScopesError.ConcurrencyError(
                aggregateId = sourceError.aggregateId,
                aggregateType = "Scope",
                expectedVersion = sourceError.eventVersion?.toInt(),
                actualVersion = sourceError.conflictingVersion?.toInt(),
                operation = "event_storage",
            )
            else -> ScopesError.RepositoryError(
                repositoryName = "EventStore",
                operation = ScopesError.RepositoryError.RepositoryOperation.SAVE,
                entityType = "Event",
                failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
            )
        }

        is EventStoreContractError.EventRetrievalError -> when (sourceError.retrievalReason) {
            EventStoreContractError.RetrievalFailureReason.NOT_FOUND -> {
                val aggregateId = sourceError.aggregateId
                val eventType = sourceError.eventType
                when {
                    aggregateId != null -> ScopesError.NotFound(
                        entityType = "Event",
                        identifier = aggregateId,
                        identifierType = "aggregateId",
                    )
                    eventType != null -> ScopesError.NotFound(
                        entityType = "Event",
                        identifier = eventType,
                        identifierType = "eventType",
                    )
                    else -> ScopesError.RepositoryError(
                        repositoryName = "EventStore",
                        operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
                        entityType = "Event",
                        failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
                    )
                }
            }
            EventStoreContractError.RetrievalFailureReason.TIMEOUT -> ScopesError.SystemError(
                errorType = ScopesError.SystemError.SystemErrorType.QUERY_TIMEOUT,
                service = "event-store",
                context = mapOf("operation" to "event_retrieval"),
            )
            else -> ScopesError.RepositoryError(
                repositoryName = "EventStore",
                operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
                entityType = "Event",
                failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
            )
        }

        is EventStoreContractError.InvalidQueryError -> ScopesError.ValidationFailed(
            field = sourceError.parameterName,
            value = sourceError.providedValue?.toString() ?: "",
            constraint = when (sourceError.constraint) {
                EventStoreContractError.QueryConstraint.MISSING_REQUIRED -> ScopesError.ValidationConstraintType.MissingRequired(
                    requiredFields = listOf(sourceError.parameterName),
                )
                EventStoreContractError.QueryConstraint.INVALID_FORMAT -> {
                    val expectedFormat = sourceError.expectedFormat
                    if (expectedFormat != null) {
                        ScopesError.ValidationConstraintType.InvalidFormat(expectedFormat = expectedFormat)
                    } else {
                        ScopesError.ValidationConstraintType.InvalidValue("Invalid format")
                    }
                }
                EventStoreContractError.QueryConstraint.OUT_OF_RANGE -> ScopesError.ValidationConstraintType.InvalidValue(
                    "Value out of range",
                )
                EventStoreContractError.QueryConstraint.INVALID_COMBINATION -> ScopesError.ValidationConstraintType.InvalidValue(
                    "Invalid parameter combination",
                )
                EventStoreContractError.QueryConstraint.UNSUPPORTED_VALUE -> ScopesError.ValidationConstraintType.InvalidValue(
                    "Unsupported value",
                )
            },
        )
    }
}
