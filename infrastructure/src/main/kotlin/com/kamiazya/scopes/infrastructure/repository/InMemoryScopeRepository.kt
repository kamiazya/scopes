package com.kamiazya.scopes.infrastructure.repository

import com.kamiazya.scopes.domain.entity.Scope
import com.kamiazya.scopes.domain.entity.ScopeId
import com.kamiazya.scopes.domain.repository.ScopeRepository
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of ScopeRepository for initial development.
 * This will be replaced with persistent storage implementation.
 */
class InMemoryScopeRepository : ScopeRepository {
    private val scopes = ConcurrentHashMap<ScopeId, Scope>()

    override suspend fun save(scope: Scope): Scope {
        scopes[scope.id] = scope
        return scope
    }

    override suspend fun findById(id: ScopeId): Scope? = scopes[id]

    override suspend fun findAll(): List<Scope> = scopes.values.toList()

    override suspend fun findByParentId(parentId: ScopeId): List<Scope> = scopes.values.filter { it.parentId == parentId }

    override suspend fun delete(id: ScopeId): Boolean = scopes.remove(id) != null

    override suspend fun update(scope: Scope): Scope? {
        val existing = scopes[scope.id] ?: return null
        val updated = scope.copy(updatedAt = Instant.now())
        scopes[scope.id] = updated
        return updated
    }
}
