package com.kamiazya.scopes.infrastructure.repository

import arrow.core.Either
import arrow.core.raise.either
import com.kamiazya.scopes.domain.entity.Scope
import com.kamiazya.scopes.domain.entity.ScopeId
import com.kamiazya.scopes.domain.error.RepositoryError
import com.kamiazya.scopes.domain.repository.ScopeRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory implementation of ScopeRepository for initial development and testing.
 * Thread-safe implementation using mutex for concurrent access.
 * Follows functional DDD principles with Result types for explicit error handling.
 * This will be replaced with persistent storage implementation.
 */
class InMemoryScopeRepository : ScopeRepository {

    private val scopes = mutableMapOf<ScopeId, Scope>()
    private val mutex = Mutex()

    override suspend fun save(scope: Scope): Either<RepositoryError, Scope> = either {
        mutex.withLock {
            scopes[scope.id] = scope
            scope
        }
    }

    override suspend fun findById(id: ScopeId): Either<RepositoryError, Scope?> = either {
        mutex.withLock {
            scopes[id]
        }
    }

    override suspend fun findAll(): Either<RepositoryError, List<Scope>> = either {
        mutex.withLock {
            scopes.values.toList()
        }
    }

    override suspend fun findByParentId(parentId: ScopeId): Either<RepositoryError, List<Scope>> = either {
        mutex.withLock {
            scopes.values.filter { it.parentId == parentId }
        }
    }

    override suspend fun findRootScopes(): Either<RepositoryError, List<Scope>> = either {
        mutex.withLock {
            scopes.values.filter { it.parentId == null }
        }
    }

    override suspend fun deleteById(id: ScopeId): Either<RepositoryError, Unit> = either {
        mutex.withLock {
            val removed = scopes.remove(id)
            if (removed == null) {
                raise(RepositoryError.NotFound(id))
            }
        }
    }

    override suspend fun existsById(id: ScopeId): Either<RepositoryError, Boolean> = either {
        mutex.withLock {
            scopes.containsKey(id)
        }
    }

    /**
     * Utility method for testing - clear all scopes.
     */
    suspend fun clear() = mutex.withLock {
        scopes.clear()
    }

    /**
     * Utility method for testing - get current scope count.
     */
    suspend fun size(): Int = mutex.withLock {
        scopes.size
    }
}

