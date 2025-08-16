package io.github.kamiazya.scopes.infrastructure.repository

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.domain.error.PersistenceError
import io.github.kamiazya.scopes.domain.error.currentTimestamp
import io.github.kamiazya.scopes.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.domain.valueobject.AspectKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory implementation of AspectDefinitionRepository for initial development and testing.
 * Thread-safe implementation using mutex for concurrent access.
 */
class InMemoryAspectDefinitionRepository : AspectDefinitionRepository {

    private val definitions = mutableMapOf<AspectKey, AspectDefinition>()
    private val mutex = Mutex()

    override suspend fun save(definition: AspectDefinition): Either<PersistenceError, AspectDefinition> = either {
        mutex.withLock {
            try {
                definitions[definition.key] = definition
                definition
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "save", e))
            }
        }
    }

    override suspend fun findByKey(key: AspectKey): Either<PersistenceError, AspectDefinition?> = either {
        mutex.withLock {
            try {
                definitions[key]
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "findByKey", e))
            }
        }
    }

    override suspend fun existsByKey(key: AspectKey): Either<PersistenceError, Boolean> = either {
        mutex.withLock {
            try {
                definitions.containsKey(key)
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "existsByKey", e))
            }
        }
    }

    override suspend fun findAll(): Either<PersistenceError, List<AspectDefinition>> = either {
        mutex.withLock {
            try {
                definitions.values.toList()
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "findAll", e))
            }
        }
    }

    override suspend fun deleteByKey(key: AspectKey): Either<PersistenceError, Boolean> = either {
        mutex.withLock {
            try {
                definitions.remove(key) != null
            } catch (e: Exception) {
                raise(PersistenceError.StorageUnavailable(currentTimestamp(), "deleteByKey", e))
            }
        }
    }

    /**
     * Utility method for testing - clear all definitions.
     */
    suspend fun clear() = mutex.withLock {
        definitions.clear()
    }

    /**
     * Utility method for testing - get current definition count.
     */
    suspend fun size(): Int = mutex.withLock {
        definitions.size
    }
}