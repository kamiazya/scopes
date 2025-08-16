package io.github.kamiazya.scopes.domain.repository

import arrow.core.Either
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.valueobject.AspectCriteria
import io.github.kamiazya.scopes.domain.error.PersistenceError

/**
 * Repository interface for Scope entity operations.
 * Follows Clean Architecture principles with basic CRUD operations.
 * Complex business logic is handled by domain services.
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
     */
    suspend fun findAll(): Either<PersistenceError, List<Scope>>

    /**
     * Find scopes by parent ID.
     */
    suspend fun findByParentId(parentId: ScopeId?): Either<PersistenceError, List<Scope>>

    /**
     * Find scopes by aspect criteria.
     */
    suspend fun findByCriteria(criteria: AspectCriteria): Either<PersistenceError, List<Scope>>

    /**
     * Check if a scope exists.
     */
    suspend fun existsById(id: ScopeId): Either<PersistenceError, Boolean>

    /**
     * Check if a scope exists with the given title and parent.
     * Used for efficient title uniqueness validation.
     */
    suspend fun existsByParentIdAndTitle(parentId: ScopeId?, title: String): Either<PersistenceError, Boolean>

    /**
     * Delete a scope by ID.
     */
    suspend fun deleteById(id: ScopeId): Either<PersistenceError, Unit>
}
