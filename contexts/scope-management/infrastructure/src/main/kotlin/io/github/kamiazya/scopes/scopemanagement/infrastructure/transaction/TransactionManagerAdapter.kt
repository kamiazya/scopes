package io.github.kamiazya.scopes.scopemanagement.infrastructure.transaction

import arrow.core.Either
import io.github.kamiazya.scopes.platform.application.port.TransactionManager as PlatformTransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager as ScopeManagementTransactionManager

/**
 * Adapter that wraps the platform TransactionManager to implement the scope management TransactionManager interface.
 */
class TransactionManagerAdapter(private val platformTransactionManager: PlatformTransactionManager) : ScopeManagementTransactionManager {

    override suspend fun <E, T> inTransaction(block: suspend () -> Either<E, T>): Either<E, T> = platformTransactionManager.inTransaction { block() }
}
