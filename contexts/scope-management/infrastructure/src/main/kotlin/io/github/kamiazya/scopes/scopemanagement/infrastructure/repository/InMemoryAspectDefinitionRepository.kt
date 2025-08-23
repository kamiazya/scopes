package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.Either
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory implementation of AspectDefinitionRepository for testing and development.
 */
class InMemoryAspectDefinitionRepository : AspectDefinitionRepository {

    private val definitions = mutableMapOf<AspectKey, AspectDefinition>()
    private val mutex = Mutex()

    override suspend fun save(definition: AspectDefinition): Either<Any, AspectDefinition> = mutex.withLock {
        definitions[definition.key] = definition
        definition.right()
    }

    override suspend fun findByKey(key: AspectKey): Either<Any, AspectDefinition?> = mutex.withLock {
        definitions[key].right()
    }

    override suspend fun existsByKey(key: AspectKey): Either<Any, Boolean> = mutex.withLock {
        definitions.containsKey(key).right()
    }

    override suspend fun findAll(): Either<Any, List<AspectDefinition>> = mutex.withLock {
        definitions.values.toList().right()
    }

    override suspend fun deleteByKey(key: AspectKey): Either<Any, Boolean> = mutex.withLock {
        val existed = definitions.containsKey(key)
        definitions.remove(key)
        existed.right()
    }
}
