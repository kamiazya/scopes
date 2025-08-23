package io.github.kamiazya.scopes.scopemanagement.domain.error

import kotlinx.datetime.Instant

/**
 * Base type for all errors in the Scopes domain.
 *
 * This sealed hierarchy provides a comprehensive error model for the Scope Management
 * bounded context, ensuring type-safe error handling and rich error information.
 *
 * Error categories include:
 * - Domain validation errors (ValidationError)
 * - External service integration errors (ExternalServiceIntegrationError)
 * - Aggregate-specific errors (ScopeError, ScopeHierarchyError, etc.)
 * - Value object validation errors (ScopeInputError, AspectValidationError, etc.)
 * - Infrastructure errors (PersistenceError)
 */
sealed class ScopesError {
    /**
     * Contextual timestamp when the error occurred.
     */
    abstract val occurredAt: Instant
}
