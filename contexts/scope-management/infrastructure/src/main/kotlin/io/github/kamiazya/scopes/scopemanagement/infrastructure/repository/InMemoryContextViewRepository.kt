package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.Either
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewName
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory implementation of ContextViewRepository for testing and development.
 */
class InMemoryContextViewRepository : ContextViewRepository {

    private val contextViews = mutableMapOf<ContextViewId, ContextView>()
    private val keyIndex = mutableMapOf<String, ContextViewId>()
    private val mutex = Mutex()

    override suspend fun save(contextView: ContextView): Either<Any, ContextView> = mutex.withLock {
        // Check for duplicate key if this is a new context view
        val existingId = keyIndex[contextView.key.value]
        if (existingId != null && existingId != contextView.id) {
            // Key already exists for a different context view
            return@withLock Either.Left("Context view with key '${contextView.key.value}' already exists")
        }

        // Remove old key from index if updating
        val oldContextView = contextViews[contextView.id]
        if (oldContextView != null && oldContextView.key != contextView.key) {
            keyIndex.remove(oldContextView.key.value)
        }

        contextViews[contextView.id] = contextView
        keyIndex[contextView.key.value] = contextView.id
        contextView.right()
    }

    override suspend fun findById(id: ContextViewId): Either<Any, ContextView?> = mutex.withLock {
        contextViews[id].right()
    }

    override suspend fun findByKey(key: ContextViewKey): Either<Any, ContextView?> = mutex.withLock {
        val id = keyIndex[key.value]
        val contextView = id?.let { contextViews[it] }
        contextView.right()
    }

    override suspend fun findByName(name: ContextViewName): Either<Any, ContextView?> = mutex.withLock {
        contextViews.values.find { it.name.value == name.value }.right()
    }

    override suspend fun findAll(): Either<Any, List<ContextView>> = mutex.withLock {
        contextViews.values.sortedBy { it.name.value }.toList().right()
    }

    override suspend fun deleteById(id: ContextViewId): Either<Any, Boolean> = mutex.withLock {
        val contextView = contextViews[id]
        if (contextView != null) {
            contextViews.remove(id)
            keyIndex.remove(contextView.key.value)
            true.right()
        } else {
            false.right()
        }
    }

    override suspend fun existsByKey(key: ContextViewKey): Either<Any, Boolean> = mutex.withLock {
        keyIndex.containsKey(key.value).right()
    }

    override suspend fun existsByName(name: ContextViewName): Either<Any, Boolean> = mutex.withLock {
        contextViews.values.any { it.name.value == name.value }.right()
    }
}
