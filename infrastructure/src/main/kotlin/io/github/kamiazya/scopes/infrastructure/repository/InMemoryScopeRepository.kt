package io.github.kamiazya.scopes.infrastructure.repository

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.error.RepositoryError
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory implementation of ScopeRepository for initial development and testing.
 * Thread-safe implementation using mutex for concurrent access.
 * Follows functional DDD principles with Result types for explicit error handling.
 * This will be replaced with persistent storage implementation.
 */
@Suppress("TooManyFunctions")
class InMemoryScopeRepository : ScopeRepository {

    private val scopes = mutableMapOf<ScopeId, Scope>()
    private val mutex = Mutex()

    override suspend fun save(scope: Scope): Either<RepositoryError, Scope> = either {
        mutex.withLock {
            scopes[scope.id] = scope
            scope
        }
    }

    override suspend fun existsById(id: ScopeId): Either<RepositoryError, Boolean> = either {
        mutex.withLock {
            scopes.containsKey(id)
        }
    }

    override suspend fun existsByParentIdAndTitle(
        parentId: ScopeId?,
        title: String
    ): Either<RepositoryError, Boolean> = either {
        mutex.withLock {
            val normalizedInputTitle = normalizeTitle(title)
            scopes.values.any { scope ->
                val normalizedStoredTitle = normalizeTitle(scope.title.value)
                scope.parentId == parentId && normalizedStoredTitle == normalizedInputTitle
            }
        }
    }

    override suspend fun countByParentId(parentId: ScopeId): Either<RepositoryError, Int> = either {
        mutex.withLock {
            scopes.values.count { it.parentId == parentId }
        }
    }

    override suspend fun findHierarchyDepth(scopeId: ScopeId): Either<RepositoryError, Int> = either {
        mutex.withLock {
            tailrec fun calculateDepth(currentId: ScopeId?, depth: Int): Int =
                when (currentId) {
                    null -> depth
                    else -> {
                        val scope = scopes[currentId]
                        if (scope == null) depth + 1 else calculateDepth(scope.parentId, depth + 1)
                    }
                }
            calculateDepth(scopeId, 0)
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

    /**
     * Normalizes a title for consistent comparison by:
     * - Trimming leading/trailing whitespace
     * - Collapsing internal whitespace sequences (spaces, tabs, newlines) to single spaces
     * - Converting to lowercase
     */
    private fun normalizeTitle(title: String): String {
        return title.trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()
    }
}

