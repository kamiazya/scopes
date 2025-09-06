package io.github.kamiazya.scopes.scopemanagement.domain.error

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Base type for all errors in the Scopes domain.
 *
 * This sealed hierarchy provides a comprehensive error model for the Scope Management
 * bounded context, ensuring type-safe error handling and rich error information.
 *
 * Error categories include:
 * - Value object validation errors (ScopeInputError, AspectValidationError)
 * - External integration errors (UserPreferencesIntegrationError)
 * - Policy errors (HierarchyPolicyError)
 * - Aggregate-specific errors (ScopeError, ScopeHierarchyError, ScopeUniquenessError)
 * - Infrastructure errors (PersistenceError)
 */
sealed class ScopesError {
    /**
     * Contextual timestamp when the error occurred.
     */
    abstract val occurredAt: Instant

    /**
     * Error for invalid operations.
     */
    data class InvalidOperation(
        val operation: String,
        val entityType: String? = null,
        val entityId: String? = null,
        val reason: InvalidOperationReason? = null,
        override val occurredAt: Instant = Clock.System.now(),
    ) : ScopesError() {
        enum class InvalidOperationReason {
            INVALID_STATE,
            OPERATION_NOT_ALLOWED,
            MISSING_PREREQUISITE,
            INVALID_INPUT,
        }
    }

    /**
     * Error indicating that an entity already exists.
     */
    data class AlreadyExists(
        val entityType: String,
        val identifier: String,
        val identifierType: String = "key", // key, id, alias, etc.
        override val occurredAt: Instant = Clock.System.now(),
    ) : ScopesError()

    /**
     * Error indicating that an entity was not found.
     */
    data class NotFound(
        val entityType: String,
        val identifier: String,
        val identifierType: String = "key", // key, id, alias, etc.
        override val occurredAt: Instant = Clock.System.now(),
    ) : ScopesError()

    /**
     * Error indicating a system-level failure.
     */
    data class SystemError(
        val errorType: SystemErrorType,
        val service: String? = null,
        val cause: Throwable? = null,
        val context: Map<String, Any> = emptyMap(),
        override val occurredAt: Instant = Clock.System.now(),
    ) : ScopesError() {
        enum class SystemErrorType {
            SERVICE_UNAVAILABLE,
            SERIALIZATION_FAILED,
            DESERIALIZATION_FAILED,
            QUERY_TIMEOUT,
            CAPACITY_EXCEEDED,
            ACCESS_DENIED,
            CONFIGURATION_ERROR,
            EXTERNAL_SERVICE_ERROR,
        }
    }

    /**
     * Error indicating a validation failure.
     */
    data class ValidationFailed(
        val field: String,
        val value: String,
        val constraint: ValidationConstraintType,
        val details: Map<String, Any> = emptyMap(),
        override val occurredAt: Instant = Clock.System.now(),
    ) : ScopesError()

    /**
     * Validation constraint types.
     */
    sealed interface ValidationConstraintType {
        data class InvalidType(val expectedType: String, val actualType: String) : ValidationConstraintType
        data class NotInAllowedValues(val allowedValues: List<String>) : ValidationConstraintType
        data class InvalidFormat(val expectedFormat: String) : ValidationConstraintType
        data class MissingRequired(val requiredFields: List<String>) : ValidationConstraintType
        data class MultipleValuesNotAllowed(val field: String) : ValidationConstraintType
        data class InvalidValue(val reason: String) : ValidationConstraintType
    }

    /**
     * Error indicating a resource conflict.
     */
    data class Conflict(
        val resourceType: String,
        val resourceId: String,
        val conflictType: ConflictType,
        val details: Map<String, Any> = emptyMap(),
        override val occurredAt: Instant = Clock.System.now(),
    ) : ScopesError() {
        enum class ConflictType {
            ALREADY_IN_USE,
            HAS_DEPENDENCIES,
            DUPLICATE_KEY,
            OPTIMISTIC_LOCK_FAILURE,
        }
    }

    /**
     * Error for concurrency conflicts during event sourcing operations.
     */
    data class ConcurrencyError(
        val aggregateId: String,
        val aggregateType: String,
        val expectedVersion: Int? = null,
        val actualVersion: Int? = null,
        val operation: String? = null,
        override val occurredAt: Instant = Clock.System.now(),
    ) : ScopesError()

    /**
     * Error for repository-level operations.
     */
    data class RepositoryError(
        val repositoryName: String,
        val operation: RepositoryOperation,
        val entityType: String? = null,
        val entityId: String? = null,
        val cause: Throwable? = null,
        override val occurredAt: Instant = Clock.System.now(),
    ) : ScopesError() {
        enum class RepositoryOperation {
            SAVE,
            FIND,
            DELETE,
            UPDATE,
            QUERY,
            COUNT,
        }
    }
}
