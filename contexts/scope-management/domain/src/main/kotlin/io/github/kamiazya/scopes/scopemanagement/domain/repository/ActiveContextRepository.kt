package io.github.kamiazya.scopes.scopemanagement.domain.repository

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError

/**
 * Repository interface for managing the active context.
 * Provides persistence for the currently active context view.
 */
interface ActiveContextRepository {
    /**
     * Get the currently active context view.
     * Returns null if no context is active.
     */
    suspend fun getActiveContext(): Either<ScopesError, ContextView?>

    /**
     * Set the active context view.
     */
    suspend fun setActiveContext(contextView: ContextView): Either<ScopesError, Unit>

    /**
     * Clear the active context.
     */
    suspend fun clearActiveContext(): Either<ScopesError, Unit>

    /**
     * Check if any context is currently active.
     */
    suspend fun hasActiveContext(): Either<ScopesError, Boolean>
}
