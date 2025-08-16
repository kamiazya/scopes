package io.github.kamiazya.scopes.domain.repository

import arrow.core.Either
import io.github.kamiazya.scopes.domain.entity.ContextView
import io.github.kamiazya.scopes.domain.error.PersistenceError
import io.github.kamiazya.scopes.domain.valueobject.ContextName
import io.github.kamiazya.scopes.domain.valueobject.ContextViewId

/**
 * Repository interface for ContextView entity operations.
 * Manages persistence of named context views for filtering scopes.
 *
 * Implementation notes:
 * - All contexts are stored globally
 * - Context names must be unique
 */
interface ContextViewRepository {

    /**
     * Save a context view (create or update).
     * Returns SaveContextError.DuplicateName if name already exists.
     */
    suspend fun save(context: ContextView): Either<PersistenceError, ContextView>

    /**
     * Find a context view by its ID.
     * Returns null if not found.
     */
    suspend fun findById(id: ContextViewId): Either<PersistenceError, ContextView?>

    /**
     * Find a context view by name.
     * Name comparison should be case-insensitive.
     * Returns null if not found.
     */
    suspend fun findByName(name: ContextName): Either<PersistenceError, ContextView?>

    /**
     * Find all context views.
     * Results are sorted by name.
     */
    suspend fun findAll(): Either<PersistenceError, List<ContextView>>

    /**
     * Delete a context view by ID.
     */
    suspend fun delete(id: ContextViewId): Either<PersistenceError, Unit>

    /**
     * Check if a context name already exists.
     * Name comparison should be case-insensitive.
     */
    suspend fun existsByName(name: ContextName): Either<PersistenceError, Boolean>
}
