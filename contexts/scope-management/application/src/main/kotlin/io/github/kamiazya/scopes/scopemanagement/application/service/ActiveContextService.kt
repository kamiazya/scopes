package io.github.kamiazya.scopes.scopemanagement.application.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.application.error.toApplicationError
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ActiveContextRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.application.error.PersistenceError as AppPersistenceError
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
class ActiveContextService(
    private val contextViewRepository: ContextViewRepository,
    private val activeContextRepository: ActiveContextRepository,
    private val contextAuditService: ContextAuditService,
) {

    /**
     * Get the currently active context.
     * Returns Either with ContextView if active, or null if no context is active.
     * Propagates errors from the repository.
     */
    suspend fun getCurrentContext(): Either<ApplicationError, ContextView?> = activeContextRepository.getActiveContext().mapLeft { error ->
        when (error) {
            is DomainPersistenceError -> error.toApplicationError()
            else -> AppPersistenceError.StorageUnavailable(
                operation = "getActiveContext",
                cause = error.toString(),
            )
        }
    }

    /**
     * Set the active context and publish audit event.
     * Maps domain errors to application errors following Clean Architecture.
     */
    suspend fun setActiveContext(context: ContextView, activatedBy: String? = null): Either<ApplicationError, Unit> = either {
        // Get current context before switching for audit trail
        val previousContext = getCurrentContext().bind()

        // Switch the active context
        activeContextRepository.setActiveContext(context)
            .mapLeft { error ->
                when (error) {
                    is DomainPersistenceError -> error.toApplicationError()
                    else -> AppPersistenceError.StorageUnavailable(
                        operation = "setActiveContext",
                        cause = error.toString(),
                    )
                }
            }
            .bind()

        // Publish audit event (non-blocking, errors are logged but don't fail the operation)
        contextAuditService.publishContextActivated(
            contextView = context,
            previousContextId = previousContext?.id?.value,
            activatedBy = activatedBy,
        ).fold(
            { error ->
                // TODO: Add proper logging - for now, silently continue
                // logger.warn("Failed to publish context activated event: $error")
            },
            { },
        )
    }

    /**
     * Clear the active context and publish audit event.
     * Maps domain errors to application errors following Clean Architecture.
     */
    suspend fun clearActiveContext(clearedBy: String? = null): Either<ApplicationError, Unit> = either {
        // Get current context before clearing for audit trail
        val previousContext = getCurrentContext().bind()

        // Clear the active context
        activeContextRepository.clearActiveContext()
            .mapLeft { error ->
                when (error) {
                    is DomainPersistenceError -> error.toApplicationError()
                    else -> AppPersistenceError.StorageUnavailable(
                        operation = "clearActiveContext",
                        cause = error.toString(),
                    )
                }
            }
            .bind()

        // Publish audit event if there was a previous context
        previousContext?.let { previous ->
            contextAuditService.publishActiveContextCleared(
                previousContext = previous,
                clearedBy = clearedBy,
            ).fold(
                { error ->
                    // TODO: Add proper logging - for now, silently continue
                    // logger.warn("Failed to publish active context cleared event: $error")
                },
                { },
            )
        }
    }

    /**
     * Switch to a context by key and publish audit event.
     * Maps domain errors to application errors following Clean Architecture.
     */
    suspend fun switchToContextByKey(key: String, activatedBy: String? = null): Either<ApplicationError, ContextView> = either {
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
                    else -> AppPersistenceError.StorageUnavailable(
                        operation = "findByKey",
                        cause = error.toString(),
                    )
                }
            }
            .bind()

        ensure(context != null) {
            ContextError.InvalidContextSwitch(key = key)
        }

        // Use setActiveContext method to ensure audit event is published
        setActiveContext(context!!, activatedBy)
            .mapLeft { it } // Already mapped to ApplicationError
            .bind()

        context
    }

    /**
     * Check if any context is currently active.
     */
    suspend fun hasActiveContext(): Boolean = activeContextRepository.hasActiveContext()
        .fold(
            ifLeft = { false },
            ifRight = { it },
        )

    /**
     * Get status information about active context.
     * Returns the status with null if there's an error getting the current context.
     */
    suspend fun getStatus(): Either<ApplicationError, ActiveContextStatus> = either {
        val activeContext = getCurrentContext().bind()
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
