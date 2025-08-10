package io.github.kamiazya.scopes.domain.repository

import arrow.core.Either
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.error.SaveScopeError
import io.github.kamiazya.scopes.domain.error.ExistsScopeError
import io.github.kamiazya.scopes.domain.error.CountScopeError
import io.github.kamiazya.scopes.domain.error.FindScopeError

/**
 * Repository interface for Scope entity operations.
 * Updated to use operation-specific error types for better type safety and error handling.
 * Each operation now uses its specific error type rather than the generic RepositoryError.
 */
interface ScopeRepository {
    /**
     * Save a scope (create or update).
     * Uses SaveScopeError for operation-specific error scenarios.
     */
    suspend fun save(scope: Scope): Either<SaveScopeError, Scope>

    /**
     * Check if a scope exists.
     * Uses ExistsScopeError for operation-specific error scenarios.
     */
    suspend fun existsById(id: ScopeId): Either<ExistsScopeError, Boolean>

    /**
     * Check if a scope exists with the given title and parent.
     * Used for efficient title uniqueness validation.
     * Uses ExistsScopeError for operation-specific error scenarios.
     */
    suspend fun existsByParentIdAndTitle(parentId: ScopeId?, title: String): Either<ExistsScopeError, Boolean>

    /**
     * Count children of a specific parent scope.
     * Used for efficient children limit validation.
     * Uses CountScopeError for operation-specific error scenarios.
     */
    suspend fun countByParentId(parentId: ScopeId): Either<CountScopeError, Int>

    /**
     * Find the depth of a scope hierarchy from root to the given scope.
     * Used for efficient hierarchy depth validation.
     * Uses FindScopeError for operation-specific error scenarios.
     */
    suspend fun findHierarchyDepth(scopeId: ScopeId): Either<FindScopeError, Int>
}

