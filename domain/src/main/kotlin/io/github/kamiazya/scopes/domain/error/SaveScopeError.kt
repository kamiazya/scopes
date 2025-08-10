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
     * Represents a unified persistence/database error during save operation.
     * Consolidates both persistence layer failures and database-level errors
     * to simplify error handling and eliminate ambiguity.
     */
    data class PersistenceError(
        val scopeId: ScopeId,
        val message: String,
        val cause: Throwable,
        val retryable: Boolean = false,
        val errorCode: String? = null,
        val category: ErrorCategory = ErrorCategory.PERSISTENCE
    ) : SaveScopeError() {
        
        enum class ErrorCategory {
            PERSISTENCE,
            DATABASE,
            CONNECTION,
            TIMEOUT,
            CONSTRAINT
        }
    }
    
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
     * Represents a system-level failure during save operation.
     * Used for cascading failures and system-wide issues without exposing infrastructure details.
     */
    data class SystemFailure(
        val scopeId: ScopeId,
        val failure: SystemFailureType,
        val retryable: Boolean,
        val correlationId: String? = null,
        val cause: Throwable? = null
    ) : SaveScopeError() {
        
        /**
         * Type-safe enumeration of system failure types.
         */
        sealed class SystemFailureType {
            data class DatabaseConnectionError(val details: String) : SystemFailureType()
            data class DatabaseQueryError(val query: String? = null) : SystemFailureType()
            data class NetworkError(val endpoint: String) : SystemFailureType()
            data class ExternalServiceError(val serviceName: String, val statusCode: Int? = null) : SystemFailureType()
            data class FileSystemError(val path: String, val operation: String) : SystemFailureType()
            data class ConfigurationError(val configKey: String) : SystemFailureType()
            data class ResourceExhaustedError(val resource: String) : SystemFailureType()
            data class MessagingError(val messageType: String) : SystemFailureType()
            data class UnknownError(val description: String = "Unknown system failure") : SystemFailureType()
        }
    }

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