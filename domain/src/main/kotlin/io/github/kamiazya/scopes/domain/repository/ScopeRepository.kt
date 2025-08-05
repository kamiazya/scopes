package io.github.kamiazya.scopes.domain.repository

import arrow.core.Either
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.error.RepositoryError

/**
 * Repository interface for Scope entity operations.
 * Follows Clean Architecture principles with domain-driven design.
 * Uses Arrow Either types for explicit error handling following functional DDD principles.
 */
interface ScopeRepository {
    /**
     * Save a scope (create or update).
     */
    suspend fun save(scope: Scope): Either<RepositoryError, Scope>

    /**
     * Check if a scope exists.
     */
    suspend fun existsById(id: ScopeId): Either<RepositoryError, Boolean>

    /**
     * Check if a scope exists with the given title and parent.
     * Used for efficient title uniqueness validation.
     */
    suspend fun existsByParentIdAndTitle(parentId: ScopeId?, title: String): Either<RepositoryError, Boolean>

    /**
     * Count children of a specific parent scope.
     * Used for efficient children limit validation.
     */
    suspend fun countByParentId(parentId: ScopeId): Either<RepositoryError, Int>

    /**
     * Find the depth of a scope hierarchy from root to the given scope.
     * Used for efficient hierarchy depth validation.
     */
    suspend fun findHierarchyDepth(scopeId: ScopeId): Either<RepositoryError, Int>
}

