package io.github.kamiazya.scopes.scopemanagement.infrastructure.transaction

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager

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
    override suspend fun <E, T> inTransaction(block: suspend () -> Either<E, T>): Either<E, T> = block()
}
