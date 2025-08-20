package io.github.kamiazya.scopes.domain.repository

import arrow.core.Either
import io.github.kamiazya.scopes.domain.entity.ContextView
import io.github.kamiazya.scopes.domain.error.PersistenceError
import io.github.kamiazya.scopes.domain.valueobject.ContextViewId
import io.github.kamiazya.scopes.domain.valueobject.ContextViewKey

/**
 * Repository interface for ContextView entity operations.
 * Manages persistence of named context views for filtering scopes.
 *
 * Implementation notes:
 * - All contexts are stored globally
 * - Context keys must be unique (for programmatic access)
 * - Context names are for display and can be duplicated
 */
interface ContextViewRepository {

    /**
     * Save a context view (create or update).
     * Returns SaveContextError.DuplicateKey if key already exists.
     */
    suspend fun save(context: ContextView): Either<PersistenceError, ContextView>

    /**
     * Find a context view by its ID.
     * Returns null if not found.
     */
    suspend fun findById(id: ContextViewId): Either<PersistenceError, ContextView?>

    /**
     * Find a context view by key.
     * Key comparison is case-sensitive.
     * Returns null if not found.
     */
    suspend fun findByKey(key: ContextViewKey): Either<PersistenceError, ContextView?>

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
     * Check if a context key already exists.
     * Key comparison is case-sensitive.
     */
    suspend fun existsByKey(key: ContextViewKey): Either<PersistenceError, Boolean>

    /**
     * Find context views by name.
     * Name comparison is case-insensitive and supports partial matching.
     * Returns empty list if none found.
     */
    suspend fun findByNameContaining(name: String): Either<PersistenceError, List<ContextView>>
}
