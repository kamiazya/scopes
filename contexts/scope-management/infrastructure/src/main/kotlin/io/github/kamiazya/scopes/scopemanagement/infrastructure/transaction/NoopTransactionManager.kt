package io.github.kamiazya.scopes.scopemanagement.infrastructure.transaction

import arrow.core.Either
import io.github.kamiazya.scopes.platform.application.port.TransactionContext
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import kotlin.random.Random

/**
 * No-operation implementation of TransactionManager.
 *
 * This implementation is used when transaction management is not required,
 * such as:
 * - In-memory repositories
 * - Testing scenarios
 * - Prototyping
 *
 * This implementation simply executes the block without any transaction
 * boundary management.
 */
class NoopTransactionManager : TransactionManager {

    /**
     * Execute the block without any transaction management.
     * Simply delegates to the block and returns its result.
     */
    override suspend fun <E, T> inTransaction(block: suspend TransactionContext.() -> Either<E, T>): Either<E, T> = block(NoopTransactionContext())

    override suspend fun <E, T> inReadOnlyTransaction(block: suspend TransactionContext.() -> Either<E, T>): Either<E, T> = block(NoopTransactionContext())
}

/**
 * No-operation implementation of TransactionContext.
 */
private class NoopTransactionContext : TransactionContext {
    private val transactionId = Random.nextLong().toString()
    private var markedForRollback = false

    override fun markForRollback() {
        markedForRollback = true
    }

    override fun isMarkedForRollback(): Boolean = markedForRollback

    override fun getTransactionId(): String = transactionId
}
