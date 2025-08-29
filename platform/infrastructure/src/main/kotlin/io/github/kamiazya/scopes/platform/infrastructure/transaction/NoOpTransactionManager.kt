package io.github.kamiazya.scopes.platform.infrastructure.transaction

import arrow.core.Either
import io.github.kamiazya.scopes.platform.application.port.TransactionContext
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * No-operation implementation of TransactionManager for SQLDelight.
 *
 * SQLDelight manages transactions at the database level,
 * so this implementation simply executes the provided block.
 */
class NoOpTransactionManager : TransactionManager {
    override suspend fun <E, T> inTransaction(block: suspend TransactionContext.() -> Either<E, T>): Either<E, T> = NoOpTransactionContext().block()

    override suspend fun <E, T> inReadOnlyTransaction(block: suspend TransactionContext.() -> Either<E, T>): Either<E, T> = NoOpTransactionContext().block()
}

/**
 * No-operation implementation of TransactionContext.
 */
@OptIn(ExperimentalUuidApi::class)
private class NoOpTransactionContext : TransactionContext {
    private var markedForRollback = false
    private val transactionId = Uuid.random().toString()

    override fun markForRollback() {
        markedForRollback = true
    }

    override fun isMarkedForRollback(): Boolean = markedForRollback

    override fun getTransactionId(): String = transactionId
}
