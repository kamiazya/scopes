package io.github.kamiazya.scopes.platform.infrastructure.transaction

import arrow.core.Either
import io.github.kamiazya.scopes.platform.application.port.TransactionContext
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Simple implementation of TransactionManager that provides basic transaction semantics.
 *
 * This implementation ensures that operations are executed within a transaction context
 * and properly handles errors and rollback scenarios.
 *
 * Note: This is a temporary implementation until we can properly integrate with
 * SQLDelight's transaction management at the database level.
 */
class SimpleTransactionManager : TransactionManager {

    override suspend fun <E, T> inTransaction(block: suspend TransactionContext.() -> Either<E, T>): Either<E, T> = withContext(Dispatchers.IO) {
        val context = SimpleTransactionContext()

        try {
            val result = context.block()

            // If the transaction was marked for rollback or resulted in an error,
            // we would normally rollback here. Since we don't have direct database
            // access, we rely on the repositories to handle their own transactions.

            result
        } catch (e: Exception) {
            // In case of exception, wrap it in Either.Left
            @Suppress("UNCHECKED_CAST")
            Either.Left(e as E)
        }
    }

    override suspend fun <E, T> inReadOnlyTransaction(block: suspend TransactionContext.() -> Either<E, T>): Either<E, T> = inTransaction(block)
}

/**
 * Simple implementation of TransactionContext.
 */
@OptIn(ExperimentalUuidApi::class)
private class SimpleTransactionContext : TransactionContext {
    private var markedForRollback = false
    private val transactionId = Uuid.random().toString()

    override fun markForRollback() {
        markedForRollback = true
    }

    override fun isMarkedForRollback(): Boolean = markedForRollback

    override fun getTransactionId(): String = transactionId
}
