package io.github.kamiazya.scopes.infrastructure.transaction

import arrow.core.Either
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.port.TransactionContext
import io.github.kamiazya.scopes.application.port.TransactionManager
import java.util.*

/**
 * No-operation implementation of TransactionManager.
 * This implementation executes the block without any actual transaction management,
 * making it suitable for:
 * - Testing scenarios
 * - In-memory repositories that don't require transactions
 * - Development environments
 * - Simple applications without complex transaction requirements
 */
class NoopTransactionManager : TransactionManager {
    
    override suspend fun <T> inTransaction(
        block: suspend TransactionContext.() -> Either<ApplicationError, T>
    ): Either<ApplicationError, T> {
        val context = NoopTransactionContext()
        return context.block()
    }
    
    override suspend fun <T> inReadOnlyTransaction(
        block: suspend TransactionContext.() -> Either<ApplicationError, T>
    ): Either<ApplicationError, T> {
        // For noop implementation, read-only is the same as regular transaction
        val context = NoopTransactionContext(readOnly = true)
        return context.block()
    }
}

/**
 * No-operation implementation of TransactionContext.
 * Tracks rollback state but doesn't perform actual transaction management.
 */
private class NoopTransactionContext(
    private val readOnly: Boolean = false
) : TransactionContext {
    
    private var markedForRollback = false
    private val transactionId = UUID.randomUUID().toString()
    
    override fun markForRollback() {
        markedForRollback = true
    }
    
    override fun isMarkedForRollback(): Boolean = markedForRollback
    
    override fun getTransactionId(): String = transactionId
}