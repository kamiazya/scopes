package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Scope count operation specific errors.
 * These errors represent different failure scenarios when counting scopes by parent ID.
 * Used for operations like countByParentId.
 */
sealed class CountScopeError {
    
    /**
     * Represents an aggregation timeout during count operation.
     * Occurs when counting large numbers of children takes longer than expected.
     */
    data class AggregationTimeout(val timeoutMillis: Long) : CountScopeError()
    
    /**
     * Represents a connection error during count operation.
     * Occurs when the connection to the storage system is lost or unavailable.
     */
    data class ConnectionError(
        val parentId: ScopeId,
        val retryable: Boolean = true,
        val cause: Throwable? = null
    ) : CountScopeError()
    
    /**
     * Represents an invalid parent ID error.
     * Occurs when trying to count children of a non-existent parent.
     */
    data class InvalidParentId(
        val parentId: ScopeId,
        val message: String
    ) : CountScopeError()
    
    /**
     * Represents a persistence layer failure during count operation.
     * Occurs when the underlying storage system encounters an error.
     */
    data class PersistenceFailure(
        val parentId: ScopeId,
        val message: String,
        val cause: Throwable
    ) : CountScopeError()
    
    /**
     * Represents an unexpected error during count operation.
     * Used as a fallback for any unhandled exceptions.
     */
    data class UnknownError(
        val parentId: ScopeId,
        val message: String,
        val cause: Throwable
    ) : CountScopeError()
}