package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.error.currentTimestamp
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory implementation of ScopeRepository for initial development and testing.
 * Thread-safe implementation using mutex for concurrent access.
 * Follows functional DDD principles with Result types for explicit error handling.
 * This will be replaced with persistent storage implementation.
 */
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

    override suspend fun existsByParentIdAndTitle(parentId: ScopeId?, title: String): Either<PersistenceError, Boolean> = either {
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

    override suspend fun findIdByParentIdAndTitle(parentId: ScopeId?, title: String): Either<PersistenceError, ScopeId?> = either {
        mutex.withLock {
            try {
                // Create a temporary ScopeTitle to get normalized value for comparison
                val inputTitle = ScopeTitle.create(title).getOrNull()
                if (inputTitle == null) {
                    // If the title is invalid, no scope can have it
                    null
                } else {
                    scopes.values.firstOrNull { scope ->
                        scope.parentId == parentId && scope.title.equalsIgnoreCase(inputTitle)
                    }?.id
                }
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "findIdByParentIdAndTitle", e))
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

    override suspend fun findByParentId(parentId: ScopeId?, offset: Int, limit: Int): Either<PersistenceError, List<Scope>> = either {
        mutex.withLock {
            try {
                scopes.values
                    .asSequence()
                    .filter { it.parentId == parentId }
                    .sortedBy { it.createdAt }
                    .drop(offset)
                    .take(limit)
                    .toList()
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "findByParentId(offset,limit)", e))
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

    override suspend fun countChildrenOf(parentId: ScopeId): Either<PersistenceError, Int> = either {
        mutex.withLock {
            try {
                scopes.values.count { it.parentId == parentId }
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "countChildrenOf", e))
            }
        }
    }

    override suspend fun findDescendantsOf(scopeId: ScopeId): Either<PersistenceError, List<Scope>> = either {
        mutex.withLock {
            try {
                val descendants = mutableListOf<Scope>()
                val toProcess = mutableListOf(scopeId)

                while (toProcess.isNotEmpty()) {
                    val currentId = toProcess.removeAt(0)
                    val children = scopes.values.filter { it.parentId == currentId }
                    descendants.addAll(children)
                    toProcess.addAll(children.map { it.id })
                }

                descendants
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "findDescendantsOf", e))
            }
        }
    }

    override suspend fun findAll(offset: Int, limit: Int): Either<PersistenceError, List<Scope>> = either {
        mutex.withLock {
            try {
                scopes.values
                    .sortedBy { it.createdAt }
                    .drop(offset)
                    .take(limit)
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "findAll", e))
            }
        }
    }

    override suspend fun findAllRoot(): Either<PersistenceError, List<Scope>> = either {
        mutex.withLock {
            try {
                scopes.values.filter { it.parentId == null }.toList()
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "findAllRoot", e))
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
