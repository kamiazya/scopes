package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.Either
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ActiveContextRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory implementation of ActiveContextRepository for testing.
 * Stores the currently active context in memory.
 */
class InMemoryActiveContextRepository : ActiveContextRepository {

    private var activeContext: ContextView? = null
    private val mutex = Mutex()

    override suspend fun getActiveContext(): Either<ScopesError, ContextView?> = mutex.withLock {
        activeContext.right()
    }

    override suspend fun setActiveContext(contextView: ContextView): Either<ScopesError, Unit> = mutex.withLock {
        activeContext = contextView
        Unit.right()
    }

    override suspend fun clearActiveContext(): Either<ScopesError, Unit> = mutex.withLock {
        activeContext = null
        Unit.right()
    }

    override suspend fun hasActiveContext(): Either<ScopesError, Boolean> = mutex.withLock {
        (activeContext != null).right()
    }
}
