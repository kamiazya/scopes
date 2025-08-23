package io.github.kamiazya.scopes.scopemanagement.domain.repository

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewName

/**
 * Repository interface for managing ContextView persistence.
 * Implementations handle the actual storage mechanism (in-memory, database, etc.).
 */
interface ContextViewRepository {

    /**
     * Save a context view.
     * Creates new or updates existing based on ID.
     */
    suspend fun save(contextView: ContextView): Either<Any, ContextView>

    /**
     * Find a context view by its unique ID.
     */
    suspend fun findById(id: ContextViewId): Either<Any, ContextView?>

    /**
     * Find a context view by its unique key.
     */
    suspend fun findByKey(key: ContextViewKey): Either<Any, ContextView?>

    /**
     * Find a context view by its display name.
     * Note: Names might not be unique, returns first match.
     */
    suspend fun findByName(name: ContextViewName): Either<Any, ContextView?>

    /**
     * List all context views.
     * Returns empty list if none exist.
     */
    suspend fun findAll(): Either<Any, List<ContextView>>

    /**
     * Delete a context view by ID.
     * Returns true if deleted, false if not found.
     */
    suspend fun deleteById(id: ContextViewId): Either<Any, Boolean>

    /**
     * Check if a context view exists with the given key.
     */
    suspend fun existsByKey(key: ContextViewKey): Either<Any, Boolean>

    /**
     * Check if a context view exists with the given name.
     */
    suspend fun existsByName(name: ContextViewName): Either<Any, Boolean>
}
