package io.github.kamiazya.scopes.scopemanagement.application.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.scopemanagement.application.error.*
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError as DomainPersistenceError

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
class ActiveContextService(private val contextViewRepository: ContextViewRepository) {
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
     * Maps domain errors to application errors following Clean Architecture.
     */
    suspend fun setActiveContext(context: ContextView): Either<ApplicationError, Unit> = either {
        mutex.withLock {
            try {
                activeContext = context
            } catch (e: Exception) {
                raise(
                    PersistenceError.StorageUnavailable(
                        operation = "setActiveContext",
                        cause = e.message,
                    ),
                )
            }
        }
    }

    /**
     * Clear the active context.
     * Maps domain errors to application errors following Clean Architecture.
     */
    suspend fun clearActiveContext(): Either<ApplicationError, Unit> = either {
        mutex.withLock {
            try {
                activeContext = null
            } catch (e: Exception) {
                raise(
                    PersistenceError.StorageUnavailable(
                        operation = "clearActiveContext",
                        cause = e.message,
                    ),
                )
            }
        }
    }

    /**
     * Switch to a context by key.
     * Maps domain errors to application errors following Clean Architecture.
     */
    suspend fun switchToContextByKey(key: String): Either<ApplicationError, ContextView> = either {
        mutex.withLock {
            try {
                val contextKey = io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey.create(key)
                    .mapLeft { errorMsg ->
                        ContextError.KeyInvalidFormat(
                            attemptedKey = key,
                        )
                    }
                    .bind()

                val context = contextViewRepository.findByKey(contextKey)
                    .mapLeft { error ->
                        when (error) {
                            is DomainPersistenceError -> error.toApplicationError()
                            else -> PersistenceError.StorageUnavailable(
                                operation = "findByKey",
                                cause = error.toString(),
                            )
                        }
                    }
                    .bind()

                ensure(context != null) {
                    ContextError.StateNotFound(
                        contextId = key,
                    )
                }

                activeContext = context
                context
            } catch (e: Exception) {
                raise(
                    PersistenceError.StorageUnavailable(
                        operation = "switchToContextByKey",
                        cause = e.message,
                    ),
                )
            }
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
            activeContext = activeContext,
        )
    }

    /**
     * Data class containing information about currently active context.
     */
    data class ActiveContextStatus(val activeContext: ContextView?) {
        val hasActiveContext: Boolean
            get() = activeContext != null
    }
}
