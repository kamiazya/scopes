package io.github.kamiazya.scopes.application.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.domain.error.ScopesError
import io.github.kamiazya.scopes.domain.error.PersistenceError
import io.github.kamiazya.scopes.domain.error.ContextError
import io.github.kamiazya.scopes.domain.error.ContextStateError
import io.github.kamiazya.scopes.domain.error.currentTimestamp
import io.github.kamiazya.scopes.domain.entity.ContextView
import io.github.kamiazya.scopes.domain.repository.ContextViewRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Service for managing the currently active context view.
 * This service maintains the state of which context is currently active
 * for filtering scopes.
 *
 * Thread-safe implementation using Mutex for concurrent access.
 *
 * Business rules:
 * - Only one context can be active at a time
 * - All contexts are global now (no scope differentiation)
 * - Falls back to "all" filter if no context is available
 */
class ActiveContextService(
    private val contextViewRepository: ContextViewRepository
) {
    private val mutex = Mutex()
    private var activeContext: ContextView? = null

    /**
     * Get the currently active context.
     * Returns null if no context is active.
     */
    suspend fun getCurrentContext(): ContextView? = mutex.withLock {
        activeContext
    }

    /**
     * Set the active context.
     */
    suspend fun setActiveContext(context: ContextView): Either<ScopesError, Unit> =
        mutex.withLock {
            try {
                activeContext = context
                Unit.right()
            } catch (e: Exception) {
                PersistenceError.StorageUnavailable(
                    currentTimestamp(),
                    "setActiveContext",
                    e
                ).left()
            }
        }

    /**
     * Clear the active context.
     */
    suspend fun clearActiveContext(): Either<ScopesError, Unit> =
        mutex.withLock {
            try {
                activeContext = null
                Unit.right()
            } catch (e: Exception) {
                PersistenceError.StorageUnavailable(
                    currentTimestamp(),
                    "clearActiveContext",
                    e
                ).left()
            }
        }


    /**
     * Switch to a context by name.
     */
    suspend fun switchToContextByName(
        name: String
    ): Either<ScopesError, ContextView> = mutex.withLock {
        try {
            val contextName = io.github.kamiazya.scopes.domain.valueobject.ContextName.create(name).getOrNull()
                ?: return@withLock ContextError.NamingError.InvalidFormat(
                    currentTimestamp(),
                    name
                ).left()
            
            val context = contextViewRepository.findByName(contextName).fold(
                ifLeft = { error -> return@withLock error.left() },
                ifRight = { it }
            )

            if (context != null) {
                activeContext = context
                return@withLock context.right()
            }

            ContextStateError.NotFound(
                currentTimestamp(),
                null,
                name
            ).left()
        } catch (e: Exception) {
            PersistenceError.StorageUnavailable(
                currentTimestamp(),
                "switchToContextByName",
                e
            ).left()
        }
    }

    /**
     * Check if any context is currently active.
     */
    suspend fun hasActiveContext(): Boolean = mutex.withLock {
        activeContext != null
    }

    /**
     * Get status information about active context.
     */
    suspend fun getStatus(): ActiveContextStatus = mutex.withLock {
        ActiveContextStatus(
            activeContext = activeContext
        )
    }

    /**
     * Data class containing information about currently active context.
     */
    data class ActiveContextStatus(
        val activeContext: ContextView?
    ) {
        val hasActiveContext: Boolean
            get() = activeContext != null
    }
}
