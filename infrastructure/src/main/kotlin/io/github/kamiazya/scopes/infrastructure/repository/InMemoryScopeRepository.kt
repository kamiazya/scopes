package io.github.kamiazya.scopes.infrastructure.repository

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.error.PersistenceError
import io.github.kamiazya.scopes.domain.error.currentTimestamp
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.valueobject.AspectCriteria
import io.github.kamiazya.scopes.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.valueobject.ScopeTitle
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

    override suspend fun save(scope: Scope): Either<PersistenceError, Scope> = either {
        mutex.withLock {
            try {
                scopes[scope.id] = scope
                scope
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "save", e))
            }
        }
    }

    override suspend fun existsById(id: ScopeId): Either<PersistenceError, Boolean> = either {
        mutex.withLock {
            try {
                scopes.containsKey(id)
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "existsById", e))
            }
        }
    }

    override suspend fun existsByParentIdAndTitle(
        parentId: ScopeId?,
        title: String,
    ): Either<PersistenceError, Boolean> = either {
        mutex.withLock {
            try {
                // Create a temporary ScopeTitle to get normalized value for comparison
                val inputTitle = ScopeTitle.create(title).getOrNull()
                if (inputTitle == null) {
                    // If the title is invalid, it can't exist
                    false
                } else {
                    scopes.values.any { scope ->
                        scope.parentId == parentId && scope.title.equalsIgnoreCase(inputTitle)
                    }
                }
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "existsByParentIdAndTitle", e))
            }
        }
    }

    override suspend fun findByParentId(parentId: ScopeId?): Either<PersistenceError, List<Scope>> = either {
        mutex.withLock {
            try {
                scopes.values.filter { it.parentId == parentId }.toList()
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "findByParentId", e))
            }
        }
    }

    override suspend fun deleteById(id: ScopeId): Either<PersistenceError, Unit> = either {
        mutex.withLock {
            try {
                scopes.remove(id)
                Unit
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "deleteById", e))
            }
        }
    }

    override suspend fun findById(id: ScopeId): Either<PersistenceError, Scope?> = either {
        mutex.withLock {
            try {
                scopes[id]
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "findById", e))
            }
        }
    }

    override suspend fun findAll(): Either<PersistenceError, List<Scope>> = either {
        mutex.withLock {
            try {
                scopes.values.toList()
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "findAll", e))
            }
        }
    }

    override suspend fun findByCriteria(criteria: AspectCriteria): Either<PersistenceError, List<Scope>> = either {
        mutex.withLock {
            try {
                // For now, we need to create empty definitions map
                // In production, this would be loaded from configuration or database
                val definitions = emptyMap<AspectKey, AspectDefinition>()

                val matchingScopes = scopes.values.filter { scope ->
                    // Use the new evaluateWithAspects method that supports multiple values
                    criteria.evaluateWithAspects(scope.aspects, definitions)
                }
                matchingScopes
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "findByCriteria", e))
            }
        }
    }

    override suspend fun update(scope: Scope): Either<PersistenceError, Scope> = either {
        mutex.withLock {
            try {
                // For in-memory implementation, update is the same as save
                // In a real database implementation, this might check for existence first
                scopes[scope.id] = scope
                scope
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "update", e))
            }
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
