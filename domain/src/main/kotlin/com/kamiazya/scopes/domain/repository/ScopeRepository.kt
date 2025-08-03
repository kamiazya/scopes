package com.kamiazya.scopes.domain.repository

import arrow.core.Either
import com.kamiazya.scopes.domain.entity.Scope
import com.kamiazya.scopes.domain.entity.ScopeId
import com.kamiazya.scopes.domain.error.RepositoryError

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
     * Find a scope by its identifier.
     */
    suspend fun findById(id: ScopeId): Either<RepositoryError, Scope?>

    /**
     * Find all scopes in the system.
     */
    suspend fun findAll(): Either<RepositoryError, List<Scope>>

    /**
     * Find all scopes that have the specified parent.
     */
    suspend fun findByParentId(parentId: ScopeId): Either<RepositoryError, List<Scope>>

    /**
     * Find all root scopes (no parent).
     */
    suspend fun findRootScopes(): Either<RepositoryError, List<Scope>>

    /**
     * Delete a scope by ID.
     */
    suspend fun deleteById(id: ScopeId): Either<RepositoryError, Unit>

    /**
     * Check if a scope exists.
     */
    suspend fun existsById(id: ScopeId): Either<RepositoryError, Boolean>
}

