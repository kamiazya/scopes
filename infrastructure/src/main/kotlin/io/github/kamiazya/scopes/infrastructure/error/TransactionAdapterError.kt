package io.github.kamiazya.scopes.infrastructure.error

import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * Transaction adapter errors for distributed transaction management.
 * Covers rollback, isolation, and coordination issues.
 */
sealed class TransactionAdapterError : InfrastructureAdapterError() {
    
    /**
     * Transaction coordination errors in distributed systems.
     */
    data class CoordinationError(
        val transactionId: String,
        val coordinationType: CoordinationType,
        val participants: List<String>,
        val failedParticipant: String?,
        val cause: Throwable,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : TransactionAdapterError() {
        // Saga pattern allows retry, but 2PC doesn't
        override val retryable: Boolean = coordinationType == CoordinationType.SAGA
    }
    
    /**
     * Transaction rollback failures.
     */
    data class RollbackError(
        val transactionId: String,
        val reason: String,
        val partialRollback: Boolean,
        val affectedResources: List<String>,
        val cause: Throwable,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : TransactionAdapterError() {
        // Rollback failures indicate persistent state issues
        override val retryable: Boolean = false
    }
    
    /**
     * Transaction isolation violations.
     */
    data class IsolationError(
        val transactionId: String,
        val violationType: IsolationViolationType,
        val conflictingTransactionId: String?,
        val affectedData: String,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : TransactionAdapterError() {
        // Isolation violations indicate logical errors, not retryable
        override val retryable: Boolean = false
    }
    
    /**
     * Transaction timeout errors.
     */
    data class TimeoutError(
        val transactionId: String,
        val timeout: Duration,
        val phase: String, // "prepare", "commit", "rollback"
        val participants: List<String>,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : TransactionAdapterError() {
        // Timeouts are transient and retryable
        override val retryable: Boolean = true
    }
    
    /**
     * Deadlock detection errors.
     */
    data class DeadlockError(
        val transactionId: String,
        val conflictingTransactionIds: List<String>,
        val resources: List<String>,
        val victimSelection: String,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : TransactionAdapterError() {
        // Deadlocks are transient and retryable after victim rollback
        override val retryable: Boolean = true
    }
}