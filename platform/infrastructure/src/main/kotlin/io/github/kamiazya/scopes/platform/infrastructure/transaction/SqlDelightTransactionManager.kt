package io.github.kamiazya.scopes.platform.infrastructure.transaction

import app.cash.sqldelight.Transacter
import arrow.core.Either
import arrow.core.left
import io.github.kamiazya.scopes.platform.application.port.TransactionContext
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * SQLDelight implementation of TransactionManager that properly integrates
 * with SQLDelight's transaction management system.
 *
 * This implementation ensures that all database operations within a transaction
 * are properly isolated and can be rolled back as a unit.
 *
 * @param transacter The SQLDelight database interface that provides transaction support
 */
class SqlDelightTransactionManager(private val transacter: Transacter) : TransactionManager {

    override suspend fun <E, T> inTransaction(block: suspend TransactionContext.() -> Either<E, T>): Either<E, T> = withContext(Dispatchers.IO) {
        val context = SqlDelightTransactionContext()

        try {
            // Use transactionWithResult to get a return value from the transaction
            transacter.transactionWithResult<Either<E, T>> {
                // Create a coroutine scope that inherits from the transaction's dispatcher
                val transactionScope = CoroutineScope(coroutineContext)

                // Run the block and get the result
                val result = kotlinx.coroutines.runBlocking(transactionScope.coroutineContext) {
                    context.block()
                }

                // Check if we need to rollback
                when {
                    context.isMarkedForRollback() -> {
                        rollback(result)
                    }
                    result.isLeft() -> {
                        // Rollback on error
                        rollback(result)
                    }
                    else -> result
                }
            }
        } catch (e: Exception) {
            // Handle any exceptions that escape the transaction
            @Suppress("UNCHECKED_CAST")
            e.left() as Either<E, T>
        }
    }

    override suspend fun <E, T> inReadOnlyTransaction(block: suspend TransactionContext.() -> Either<E, T>): Either<E, T> = withContext(Dispatchers.IO) {
        val context = SqlDelightTransactionContext()

        try {
            // Use transactionWithResult and always rollback for read-only
            transacter.transactionWithResult<Either<E, T>> {
                val transactionScope = CoroutineScope(coroutineContext)

                val result = kotlinx.coroutines.runBlocking(transactionScope.coroutineContext) {
                    context.block()
                }

                // Always rollback for read-only transactions
                rollback(result)
            }
        } catch (e: Exception) {
            @Suppress("UNCHECKED_CAST")
            e.left() as Either<E, T>
        }
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
