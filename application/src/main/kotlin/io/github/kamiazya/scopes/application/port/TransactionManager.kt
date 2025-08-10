package io.github.kamiazya.scopes.application.port

import arrow.core.Either
import io.github.kamiazya.scopes.application.error.ApplicationError

/**
 * Port for managing database transactions in use cases.
 * This abstraction allows different transaction implementations
 * while keeping the application layer technology-agnostic.
 */
interface TransactionManager {
    
    /**
     * Execute a block of code within a transaction.
     * The transaction will be committed if the block completes successfully,
     * or rolled back if an exception occurs or an error is returned.
     * 
     * @param block The code block to execute within the transaction
     * @return Either an ApplicationError if the transaction fails, or the result of the block
     */
    suspend fun <T> inTransaction(
        block: suspend TransactionContext.() -> Either<ApplicationError, T>
    ): Either<ApplicationError, T>
    
    /**
     * Execute a block of code within a transaction, but always roll back.
     * Useful for read-only operations or testing scenarios.
     * 
     * @param block The code block to execute within the transaction
     * @return Either an ApplicationError if the transaction fails, or the result of the block
     */
    suspend fun <T> inReadOnlyTransaction(
        block: suspend TransactionContext.() -> Either<ApplicationError, T>
    ): Either<ApplicationError, T>
}

/**
 * Context provided during transaction execution.
 * Can be used to access transaction-specific resources or operations.
 */
interface TransactionContext {
    
    /**
     * Marks the current transaction for rollback.
     * The transaction will be rolled back even if no exception occurs.
     */
    fun markForRollback()
    
    /**
     * Checks if the current transaction is marked for rollback.
     * 
     * @return true if the transaction is marked for rollback, false otherwise
     */
    fun isMarkedForRollback(): Boolean
    
    /**
     * Gets the transaction ID for logging or debugging purposes.
     * 
     * @return A unique identifier for this transaction
     */
    fun getTransactionId(): String
}