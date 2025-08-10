package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Scope existence check operation specific errors.
 * These errors represent different failure scenarios when checking if a scope exists.
 * Used for operations like existsById and existsByParentIdAndTitle.
 */
sealed class ExistsScopeError {

    /**
     * Context information for different types of existence check operations.
     */
    sealed class ExistenceContext {
        /**
         * Context for existence check by scope ID.
         */
        data class ById(val scopeId: ScopeId) : ExistenceContext()

        /**
         * Context for existence check by parent ID and title.
         */
        data class ByParentIdAndTitle(
            val parentId: ScopeId?,
            val title: String
        ) : ExistenceContext()

        /**
         * Context for existence check by custom criteria.
         */
        data class ByCustomCriteria(
            val criteria: Map<String, Any>
        ) : ExistenceContext()
    }

    /**
     * Represents a query timeout during existence check.
     * Occurs when the database query takes longer than the configured timeout.
     */
    data class QueryTimeout(
        val context: ExistenceContext,
        val timeoutMs: Long,
        val operation: String = "EXISTS_CHECK"
    ) : ExistsScopeError()

    /**
     * Represents a table lock timeout during existence check.
     * Occurs when the query cannot acquire necessary locks within the timeout period.
     */
    data class LockTimeout(
        val timeoutMs: Long,
        val operation: String,
        val retryable: Boolean = true
    ) : ExistsScopeError()

    /**
     * Represents a connection failure during existence check.
     * Occurs when the connection to the storage system is lost or unavailable.
     */
    data class ConnectionFailure(
        val message: String,
        val cause: Throwable
    ) : ExistsScopeError()

    /**
     * Represents an index corruption issue.
     * Occurs when the database index used for existence checks is corrupted.
     */
    data class IndexCorruption(
        val scopeId: ScopeId?,
        val message: String
    ) : ExistsScopeError()

    /**
     * Represents a unified persistence/database error during existence check.
     * Consolidates both persistence layer failures and database-level errors.
     */
    data class PersistenceError(
        val context: ExistenceContext,
        val message: String,
        val cause: Throwable,
        val retryable: Boolean = false,
        val errorCode: String? = null,
        val category: ErrorCategory = ErrorCategory.PERSISTENCE
    ) : ExistsScopeError() {
        
        enum class ErrorCategory {
            PERSISTENCE,
            DATABASE,
            CONNECTION,
            TIMEOUT,
            CONSTRAINT
        }
    }

    /**
     * Represents an unexpected error during existence check.
     * Used as a fallback for any unhandled exceptions.
     */
    data class UnknownError(
        val message: String,
        val cause: Throwable
    ) : ExistsScopeError()
}
