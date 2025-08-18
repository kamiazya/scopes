package io.github.kamiazya.scopes.application.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.error.DomainErrorMapper
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
     * Maps domain errors to application errors following Clean Architecture.
     */
    suspend fun setActiveContext(context: ContextView): Either<ApplicationError, Unit> = either {
        mutex.withLock {
            try {
                activeContext = context
            } catch (e: Exception) {
                raise(ApplicationError.PersistenceError.StorageUnavailable(
                    operation = "setActiveContext",
                    cause = e.message
                ))
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
                raise(ApplicationError.PersistenceError.StorageUnavailable(
                    operation = "clearActiveContext",
                    cause = e.message
                ))
            }
        }
    }


    /**
     * Switch to a context by name.
     * Maps domain errors to application errors following Clean Architecture.
     */
    suspend fun switchToContextByName(
        name: String
    ): Either<ApplicationError, ContextView> = either {
        mutex.withLock {
            try {
                val contextName = io.github.kamiazya.scopes.domain.valueobject.ContextName.create(name)
                    .mapLeft {
                        ApplicationError.ContextError.NamingInvalidFormat(
                            attemptedName = name
                        )
                    }
                    .bind()
                
                val context = contextViewRepository.findByName(contextName)
                    .mapLeft { error -> 
                        DomainErrorMapper.mapToApplicationError(error)
                    }
                    .bind()

                ensure(context != null) {
                    ApplicationError.ContextError.StateNotFound(
                        contextName = name,
                        contextId = null
                    )
                }

                activeContext = context
                context
            } catch (e: Exception) {
                raise(ApplicationError.PersistenceError.StorageUnavailable(
                    operation = "switchToContextByName",
                    cause = e.message
                ))
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
