package io.github.kamiazya.scopes.scopemanagement.domain.repository

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Repository interface for Scope entity operations.
 * Follows Clean Architecture principles with basic CRUD operations.
 * Complex business logic is handled by domain services.
 *
 * Note: Aspect-based queries are handled by the aspect-management context.
 */
interface ScopeRepository {
    /**
     * Save a scope (create or update).
     */
    suspend fun save(scope: Scope): Either<PersistenceError, Scope>

    /**
     * Find a scope by ID.
     */
    suspend fun findById(id: ScopeId): Either<PersistenceError, Scope?>

    /**
     * Find all scopes.
     *
     * Ordering: deterministic, newest first (ordered by `created_at DESC, id DESC`).
     * Use the paginated overload for large datasets.
     */
    suspend fun findAll(): Either<PersistenceError, List<Scope>>

    /**
     * Find all scopes with pagination.
     *
     * Ordering: deterministic, newest first (ordered by `created_at DESC, id DESC`).
     */
    suspend fun findAll(offset: Int, limit: Int): Either<PersistenceError, List<Scope>>

    // NOTE: Non-paginated version removed. Use paginated overload to avoid full scans.

    /**
     * Find scopes by parent ID with pagination.
     * Results are ordered by creation time ascending to provide stable paging
     * within a parent group (ties broken by `id ASC`). When parentId is null,
     * returns root scopes.
     */
    suspend fun findByParentId(parentId: ScopeId?, offset: Int, limit: Int): Either<PersistenceError, List<Scope>>

    /**
     * Find all root scopes (scopes with no parent).
     */
    suspend fun findAllRoot(): Either<PersistenceError, List<Scope>>

    /**
     * Check if a scope exists.
     */
    suspend fun existsById(id: ScopeId): Either<PersistenceError, Boolean>

    /**
     * Check if a scope exists with the given title and parent.
     * Used for efficient title uniqueness validation.
     * When parentId is null, checks for root-level title uniqueness.
     * Title uniqueness is enforced at ALL levels including root level.
     */
    suspend fun existsByParentIdAndTitle(parentId: ScopeId?, title: String): Either<PersistenceError, Boolean>

    /**
     * Find the ID of a scope with the given title and parent.
     * Used for retrieving the conflicting scope ID during uniqueness validation.
     * When parentId is null, searches for root-level scopes.
     * Returns null if no scope exists with the given title and parent.
     */
    suspend fun findIdByParentIdAndTitle(parentId: ScopeId?, title: String): Either<PersistenceError, ScopeId?>

    /**
     * Delete a scope by ID.
     */
    suspend fun deleteById(id: ScopeId): Either<PersistenceError, Unit>

    /**
     * Update an existing scope.
     * This is used by the command handler to update the read model after events are applied.
     */
    suspend fun update(scope: Scope): Either<PersistenceError, Scope>

    /**
     * Count the number of direct children of a scope.
     * Used for validating children limits.
     */
    suspend fun countChildrenOf(parentId: ScopeId): Either<PersistenceError, Int>

    /**
     * Count scopes by parent id. When parentId is null, counts root scopes.
     * Useful for pagination total counts.
     */
    suspend fun countByParentId(parentId: ScopeId?): Either<PersistenceError, Int>

    /**
     * Find all descendants of a scope recursively.
     * Used for hierarchy operations.
     *
     * Ordering: no guarantees. Consumers and tests must not rely on the
     * traversal order; treat the returned list as an unordered collection.
     */
    suspend fun findDescendantsOf(scopeId: ScopeId): Either<PersistenceError, List<Scope>>
}
