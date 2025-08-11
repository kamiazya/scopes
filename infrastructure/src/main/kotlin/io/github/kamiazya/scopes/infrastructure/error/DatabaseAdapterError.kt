package io.github.kamiazya.scopes.infrastructure.error

import kotlinx.datetime.Instant

/**
 * Database adapter errors for persistence operations.
 * Covers connection pools, queries, transactions, and data integrity issues.
 */
sealed class DatabaseAdapterError : InfrastructureAdapterError() {
    
    /**
     * Connection-related database errors including pool exhaustion and network issues.
     */
    data class ConnectionError(
        val connectionString: String,
        val poolSize: Int?,
        val activeConnections: Int?,
        val cause: Throwable,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : DatabaseAdapterError() {
        // Connection issues are typically transient (network blips, pool exhaustion)
        override val retryable: Boolean = true
    }
    
    /**
     * Query execution errors including timeouts and syntax errors.
     */
    data class QueryError(
        val query: String,
        val parameters: Map<String, Any>? = null,
        val executionTimeMs: Long?,
        val errorType: QueryErrorType = QueryErrorType.OTHER,
        val cause: Throwable,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : DatabaseAdapterError() {
        // Retry logic determined by error type - timeouts and transient errors are retryable
        override val retryable: Boolean = errorType.retryable
    }
    
    /**
     * Transaction-related errors including deadlocks and rollback failures.
     */
    data class TransactionError(
        val transactionId: String,
        val operation: TransactionOperation,
        val isolationLevel: TransactionIsolationLevel?,
        val cause: Throwable,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : DatabaseAdapterError() {
        // Only deadlocks are retryable as they represent transient resource conflicts
        // Other transaction errors (rollback failures, etc.) indicate persistent issues
        override val retryable: Boolean = operation == TransactionOperation.DEADLOCK
    }
    
    /**
     * Data integrity violations including constraint failures and corruption.
     */
    data class DataIntegrityError(
        val table: String,
        val constraint: String?,
        val violationType: String,
        val affectedData: Map<String, Any>? = null,
        val cause: Throwable,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : DatabaseAdapterError() {
        // Data integrity violations are persistent and not retryable
        override val retryable: Boolean = false
    }
    
    /**
     * Resource exhaustion errors for database resources.
     */
    data class ResourceExhaustionError(
        val resourceType: DatabaseResource,
        val currentUsage: Long,
        val limit: Long,
        val estimatedRecoveryTime: Long? = null,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : DatabaseAdapterError() {
        // Only connection exhaustion is retryable (connections can be freed)
        // Storage and memory exhaustion require intervention
        override val retryable: Boolean = resourceType == DatabaseResource.CONNECTIONS
    }
}