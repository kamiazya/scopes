package io.github.kamiazya.scopes.application.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
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
    suspend fun setActiveContext(context: ContextView): Either<ApplicationError, Unit> =
        mutex.withLock {
            try {
                activeContext = context
                Unit.right()
            } catch (e: Exception) {
                ApplicationError.PersistenceError.StorageUnavailable(
                    operation = "setActiveContext",
                    cause = e.message
                ).left()
            }
        }

    /**
     * Clear the active context.
     * Maps domain errors to application errors following Clean Architecture.
     */
    suspend fun clearActiveContext(): Either<ApplicationError, Unit> =
        mutex.withLock {
            try {
                activeContext = null
                Unit.right()
            } catch (e: Exception) {
                ApplicationError.PersistenceError.StorageUnavailable(
                    operation = "clearActiveContext",
                    cause = e.message
                ).left()
            }
        }


    /**
     * Switch to a context by name.
     * Maps domain errors to application errors following Clean Architecture.
     */
    suspend fun switchToContextByName(
        name: String
    ): Either<ApplicationError, ContextView> = mutex.withLock {
        try {
            val contextName = io.github.kamiazya.scopes.domain.valueobject.ContextName.create(name).getOrNull()
                ?: return@withLock ApplicationError.ContextError.NamingInvalidFormat(
                    attemptedName = name
                ).left()
            
            val context = contextViewRepository.findByName(contextName).fold(
                ifLeft = { error -> 
                    return@withLock DomainErrorMapper.mapToApplicationError(error).left() 
                },
                ifRight = { it }
            )

            if (context != null) {
                activeContext = context
                return@withLock context.right()
            }

            ApplicationError.ContextError.StateNotFound(
                contextName = name,
                contextId = null
            ).left()
        } catch (e: Exception) {
            ApplicationError.PersistenceError.StorageUnavailable(
                operation = "switchToContextByName",
                cause = e.message
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
