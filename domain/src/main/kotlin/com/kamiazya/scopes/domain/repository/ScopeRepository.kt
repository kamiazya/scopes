package com.kamiazya.scopes.domain.repository

import com.kamiazya.scopes.domain.entity.Scope
import com.kamiazya.scopes.domain.entity.ScopeId

/**
 * Repository interface for Scope entity operations.
 * Follows Clean Architecture principles with domain-driven design.
 */
interface ScopeRepository {
    suspend fun save(scope: Scope): Scope

    suspend fun findById(id: ScopeId): Scope?

    suspend fun findAll(): List<Scope>

    suspend fun findByParentId(parentId: ScopeId): List<Scope>

    suspend fun delete(id: ScopeId): Boolean

    suspend fun update(scope: Scope): Scope?
}
