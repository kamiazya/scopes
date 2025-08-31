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
     * Generic error for invalid operations.
     */
    data class InvalidOperation(val message: String, override val occurredAt: Instant = Clock.System.now()) : ScopesError()

    /**
     * Error indicating that an entity already exists.
     */
    data class AlreadyExists(val message: String, override val occurredAt: Instant = Clock.System.now()) : ScopesError()

    /**
     * Error indicating that an entity was not found.
     */
    data class NotFound(val message: String, override val occurredAt: Instant = Clock.System.now()) : ScopesError()

    /**
     * Error indicating a system-level failure.
     */
    data class SystemError(
        val message: String,
        val cause: Throwable? = null,
        val errorCode: String = "SYSTEM_ERROR",
        val context: Map<String, Any> = emptyMap(),
        override val occurredAt: Instant = Clock.System.now(),
    ) : ScopesError()

    /**
     * Error indicating a validation failure.
     */
    data class ValidationFailed(val message: String, val details: Any? = null, override val occurredAt: Instant = Clock.System.now()) : ScopesError()
}
