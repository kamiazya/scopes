package io.github.kamiazya.scopes.infrastructure.repository

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.error.SaveScopeError
import io.github.kamiazya.scopes.domain.error.ExistsScopeError
import io.github.kamiazya.scopes.domain.error.CountScopeError
import io.github.kamiazya.scopes.domain.error.FindScopeError
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.util.TitleNormalizer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory implementation of ScopeRepository for initial development and testing.
 * Thread-safe implementation using mutex for concurrent access.
 * Follows functional DDD principles with Result types for explicit error handling.
 * This will be replaced with persistent storage implementation.
 */
@Suppress("TooManyFunctions")
open class InMemoryScopeRepository : ScopeRepository {

    protected val scopes = mutableMapOf<ScopeId, Scope>()
    protected val mutex = Mutex()

    override suspend fun save(scope: Scope): Either<SaveScopeError, Scope> = either {
        mutex.withLock {
            try {
                // Check for duplicate ID scenario (though unlikely in in-memory implementation)
                if (scopes.containsKey(scope.id) && scopes[scope.id] != scope) {
                    raise(SaveScopeError.DuplicateId(scope.id))
                }
                scopes[scope.id] = scope
                scope
            } catch (e: Exception) {
                raise(SaveScopeError.UnknownError(scope.id, "Unexpected error during save", e))
            }
        }
    }

    override suspend fun existsById(id: ScopeId): Either<ExistsScopeError, Boolean> = either {
        mutex.withLock {
            try {
                scopes.containsKey(id)
            } catch (e: Exception) {
                raise(ExistsScopeError.UnknownError("Unexpected error during existence check", e))
            }
        }
    }

    override suspend fun existsByParentIdAndTitle(
        parentId: ScopeId?,
        title: String
    ): Either<ExistsScopeError, Boolean> = either {
        mutex.withLock {
            try {
                val normalizedInputTitle = TitleNormalizer.normalize(title)
                scopes.values.any { scope ->
                    val normalizedStoredTitle = TitleNormalizer.normalize(scope.title.value)
                    scope.parentId == parentId && normalizedStoredTitle == normalizedInputTitle
                }
            } catch (e: Exception) {
                raise(ExistsScopeError.UnknownError("Unexpected error during existence check by parent and title", e))
            }
        }
    }

    override suspend fun countByParentId(parentId: ScopeId): Either<CountScopeError, Int> = either {
        mutex.withLock {
            try {
                scopes.values.count { it.parentId == parentId }
            } catch (e: Exception) {
                raise(CountScopeError.UnknownError(parentId, "Unexpected error during count operation", e))
            }
        }
    }

    override suspend fun findHierarchyDepth(scopeId: ScopeId): Either<FindScopeError, Int> = either {
        mutex.withLock {
            val visited = mutableSetOf<ScopeId>()
            
            fun calculateDepth(currentId: ScopeId?, depth: Int): Either<FindScopeError, Int> {
                return when (currentId) {
                    null -> Either.Right(depth)
                    else -> {
                        // Check for circular reference
                        if (visited.contains(currentId)) {
                            val cyclePath = visited.toList() + currentId
                            return Either.Left(FindScopeError.CircularReference(scopeId, cyclePath))
                        }
                        visited.add(currentId)
                        
                        val scope = scopes[currentId]
                        when (scope) {
                            null -> Either.Left(FindScopeError.OrphanedScope(currentId, "Parent scope not found during traversal"))
                            else -> calculateDepth(scope.parentId, depth + 1)
                        }
                    }
                }
            }
            
            calculateDepth(scopeId, 0).bind()
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

