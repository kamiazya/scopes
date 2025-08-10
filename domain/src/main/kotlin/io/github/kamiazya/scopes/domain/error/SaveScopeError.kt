package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Scope save operation specific errors.
 * These errors represent different failure scenarios when saving a scope entity.
 * Follows functional programming principles with explicit error handling using Arrow Either.
 */
sealed class SaveScopeError {
    
    /**
     * Represents a duplicate ID conflict when trying to save a scope.
     * Occurs when attempting to create a scope with an ID that already exists.
     */
    data class DuplicateId(val scopeId: ScopeId) : SaveScopeError()
    
    /**
     * Represents an optimistic locking failure.
     * Occurs when the scope was modified by another process between read and write operations.
     */
    data class OptimisticLockFailure(
        val scopeId: ScopeId,
        val message: String
    ) : SaveScopeError()
    
    /**
     * Represents a validation failure during save operation.
     * Occurs when the scope data fails domain validation rules.
     */
    data class ValidationFailure(
        val scopeId: ScopeId,
        val message: String
    ) : SaveScopeError()
    
    /**
     * Represents a persistence layer failure.
     * Occurs when the underlying storage system encounters an error.
     */
    data class PersistenceFailure(
        val scopeId: ScopeId,
        val message: String,
        val cause: Throwable
    ) : SaveScopeError()
    
    /**
     * Represents a database-level error during save operation.
     * Maps infrastructure adapter errors to domain repository errors.
     */
    data class DatabaseError(
        val scopeId: ScopeId,
        val message: String,
        val cause: Throwable,
        val retryable: Boolean = false
    ) : SaveScopeError()
    
    /**
     * Represents a transaction error during save operation.
     * Includes deadlocks, rollbacks, and coordination failures.
     */
    data class TransactionError(
        val scopeId: ScopeId,
        val transactionId: String,
        val operation: String,
        val retryable: Boolean,
        val cause: Throwable? = null
    ) : SaveScopeError()
    
    /**
     * Represents a data integrity constraint violation.
     * Includes unique constraints, foreign key violations, and check constraints.
     */
    data class DataIntegrity(
        val scopeId: ScopeId,
        val constraint: String,
        val retryable: Boolean = false,
        val cause: Throwable? = null
    ) : SaveScopeError()
    
    /**
     * Represents a storage-related error during save operation.
     * Includes disk space, quota, and resource exhaustion issues.
     */
    data class StorageError(
        val scopeId: ScopeId,
        val availableSpace: Long,
        val requiredSpace: Long,
        val retryable: Boolean = false,
        val cause: Throwable? = null
    ) : SaveScopeError()
    
    /**
     * Represents an infrastructure-level failure.
     * Used for cascading failures and system-wide issues.
     */
    data class InfrastructureError(
        val scopeId: ScopeId,
        val failureType: String,
        val retryable: Boolean,
        val correlationId: String? = null,
        val cause: Throwable? = null
    ) : SaveScopeError()

    /**
     * Represents an unexpected error during save operation.
     * Used as a fallback for any unhandled exceptions.
     */
    data class UnknownError(
        val scopeId: ScopeId,
        val message: String,
        val cause: Throwable
    ) : SaveScopeError()
}