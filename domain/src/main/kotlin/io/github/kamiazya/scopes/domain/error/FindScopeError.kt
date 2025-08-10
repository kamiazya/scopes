package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Scope hierarchy traversal and find operation specific errors.
 * These errors represent different failure scenarios when traversing scope hierarchies.
 * Used for operations like findHierarchyDepth and other hierarchy-related queries.
 */
sealed class FindScopeError {
    
    /**
     * Represents a timeout during hierarchy traversal.
     * Occurs when traversing deep hierarchies takes longer than expected.
     */
    data class TraversalTimeout(
        val scopeId: ScopeId,
        val timeoutMillis: Long
    ) : FindScopeError()
    
    /**
     * Represents a circular reference detection in the hierarchy.
     * Occurs when a scope has itself as an ancestor, creating an infinite loop.
     */
    data class CircularReference(
        val scopeId: ScopeId,
        val cyclePath: List<ScopeId>
    ) : FindScopeError()
    
    /**
     * Represents an orphaned scope error.
     * Occurs when a scope references a parent that doesn't exist.
     */
    data class OrphanedScope(
        val scopeId: ScopeId,
        val message: String
    ) : FindScopeError()
    
    /**
     * Represents a connection failure during hierarchy traversal.
     * Occurs when the connection to the storage system is lost or unavailable.
     */
    data class ConnectionFailure(
        val scopeId: ScopeId,
        val message: String,
        val cause: Throwable
    ) : FindScopeError()
    
    /**
     * Represents a persistence layer failure during traversal.
     * Occurs when the underlying storage system encounters an error.
     */
    data class PersistenceFailure(
        val scopeId: ScopeId,
        val message: String,
        val cause: Throwable
    ) : FindScopeError()
    
    /**
     * Represents a transaction isolation violation during hierarchy traversal.
     * Occurs when concurrent modifications affect the hierarchy structure.
     */
    data class IsolationViolation(
        val scopeId: ScopeId,
        val violationType: String,
        val retryable: Boolean = true
    ) : FindScopeError()

    /**
     * Represents an unexpected error during hierarchy traversal.
     * Used as a fallback for any unhandled exceptions.
     */
    data class UnknownError(
        val scopeId: ScopeId,
        val message: String,
        val cause: Throwable
    ) : FindScopeError()
}