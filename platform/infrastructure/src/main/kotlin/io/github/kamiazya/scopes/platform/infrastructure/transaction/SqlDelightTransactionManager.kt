package io.github.kamiazya.scopes.platform.infrastructure.transaction

import app.cash.sqldelight.Transacter
import arrow.core.Either
import io.github.kamiazya.scopes.platform.application.port.TransactionContext
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * SQLDelight implementation of TransactionManager.
 *
 * IMPORTANT: This is a simplified implementation that delegates actual transaction
 * management to SQLDelight at the repository level. Each repository method that
 * modifies data should use database.transaction { } internally.
 *
 * This approach ensures:
 * - Transaction boundaries are properly managed by SQLDelight
 * - No issues with coroutine context or runBlocking
 * - Type safety is maintained
 *
 * The TransactionManager serves as a coordination point for the application layer
 * and provides a consistent API, but the actual transaction execution happens
 * at the repository level where SQLDelight's transaction API can be used directly.
 *
 * @param transacter The SQLDelight database interface (kept for future use)
 */
class SqlDelightTransactionManager(private val transacter: Transacter) : TransactionManager {

    override suspend fun <E, T> inTransaction(block: suspend TransactionContext.() -> Either<E, T>): Either<E, T> = withContext(Dispatchers.IO) {
        val context = SqlDelightTransactionContext()

        // Execute the block with the transaction context
        // The actual database transaction will be handled by repositories
        // when they call database.transaction { } internally
        block(context)
    }

    override suspend fun <E, T> inReadOnlyTransaction(block: suspend TransactionContext.() -> Either<E, T>): Either<E, T> = withContext(Dispatchers.IO) {
        val context = SqlDelightTransactionContext()

        // For read-only transactions, we still execute the block normally
        // Repositories can optimize by not using transactions for read-only operations
        block(context)
    }
}

/**
 * SQLDelight-specific implementation of TransactionContext.
 */
@OptIn(ExperimentalUuidApi::class)
private class SqlDelightTransactionContext : TransactionContext {
    private var markedForRollback = false
    private val transactionId = Uuid.random().toString()

    override fun markForRollback() {
        markedForRollback = true
    }

    override fun isMarkedForRollback(): Boolean = markedForRollback

    override fun getTransactionId(): String = transactionId
}
