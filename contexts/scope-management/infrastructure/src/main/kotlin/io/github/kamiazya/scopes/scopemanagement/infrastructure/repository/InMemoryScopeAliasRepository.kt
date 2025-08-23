package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.Either
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory implementation of ScopeAliasRepository for testing and development.
 */
class InMemoryScopeAliasRepository : ScopeAliasRepository {

    private val aliases = mutableMapOf<AliasId, ScopeAlias>()
    private val aliasNameIndex = mutableMapOf<String, AliasId>()
    private val scopeIdIndex = mutableMapOf<ScopeId, MutableSet<AliasId>>()
    private val mutex = Mutex()

    override suspend fun save(alias: ScopeAlias): Either<PersistenceError, Unit> = mutex.withLock {
        aliases[alias.id] = alias
        aliasNameIndex[alias.aliasName.value] = alias.id
        scopeIdIndex.getOrPut(alias.scopeId) { mutableSetOf() }.add(alias.id)
        Unit.right()
    }

    override suspend fun findByAliasName(aliasName: AliasName): Either<PersistenceError, ScopeAlias?> = mutex.withLock {
        val aliasId = aliasNameIndex[aliasName.value]
        val alias = aliasId?.let { aliases[it] }
        alias.right()
    }

    override suspend fun findById(aliasId: AliasId): Either<PersistenceError, ScopeAlias?> = mutex.withLock {
        aliases[aliasId].right()
    }

    override suspend fun findByScopeId(scopeId: ScopeId): Either<PersistenceError, List<ScopeAlias>> = mutex.withLock {
        val aliasIds = scopeIdIndex[scopeId] ?: emptySet()
        val aliasList = aliasIds.mapNotNull { aliases[it] }
        aliasList.right()
    }

    override suspend fun findCanonicalByScopeId(scopeId: ScopeId): Either<PersistenceError, ScopeAlias?> = mutex.withLock {
        val aliasIds = scopeIdIndex[scopeId] ?: emptySet()
        val canonicalAlias = aliasIds
            .mapNotNull { aliases[it] }
            .find { it.aliasType == AliasType.CANONICAL }
        canonicalAlias.right()
    }

    override suspend fun findByScopeIdAndType(scopeId: ScopeId, aliasType: AliasType): Either<PersistenceError, List<ScopeAlias>> = mutex.withLock {
        val aliasIds = scopeIdIndex[scopeId] ?: emptySet()
        val filteredAliases = aliasIds
            .mapNotNull { aliases[it] }
            .filter { it.aliasType == aliasType }
        filteredAliases.right()
    }

    override suspend fun findByAliasNamePrefix(prefix: String, limit: Int): Either<PersistenceError, List<ScopeAlias>> = mutex.withLock {
        val matchingAliases = aliasNameIndex.keys
            .filter { it.startsWith(prefix) }
            .take(limit)
            .mapNotNull { name -> aliasNameIndex[name]?.let { aliases[it] } }
        matchingAliases.right()
    }

    override suspend fun existsByAliasName(aliasName: AliasName): Either<PersistenceError, Boolean> = mutex.withLock {
        aliasNameIndex.containsKey(aliasName.value).right()
    }

    override suspend fun removeByAliasName(aliasName: AliasName): Either<PersistenceError, Boolean> = mutex.withLock {
        val aliasId = aliasNameIndex[aliasName.value]
        if (aliasId != null) {
            val alias = aliases[aliasId]
            if (alias != null) {
                aliases.remove(aliasId)
                aliasNameIndex.remove(aliasName.value)
                scopeIdIndex[alias.scopeId]?.remove(aliasId)
                true.right()
            } else {
                false.right()
            }
        } else {
            false.right()
        }
    }

    override suspend fun removeByScopeId(scopeId: ScopeId): Either<PersistenceError, Int> = mutex.withLock {
        val aliasIds = scopeIdIndex[scopeId] ?: emptySet()
        val count = aliasIds.size
        aliasIds.forEach { aliasId ->
            val alias = aliases[aliasId]
            if (alias != null) {
                aliases.remove(aliasId)
                aliasNameIndex.remove(alias.aliasName.value)
            }
        }
        scopeIdIndex.remove(scopeId)
        count.right()
    }

    override suspend fun update(alias: ScopeAlias): Either<PersistenceError, Boolean> = mutex.withLock {
        if (aliases.containsKey(alias.id)) {
            // Remove old name from index if it changed
            val oldAlias = aliases[alias.id]
            if (oldAlias != null && oldAlias.aliasName != alias.aliasName) {
                aliasNameIndex.remove(oldAlias.aliasName.value)
            }

            // Update the alias
            aliases[alias.id] = alias
            aliasNameIndex[alias.aliasName.value] = alias.id
            true.right()
        } else {
            false.right()
        }
    }

    override suspend fun count(): Either<PersistenceError, Long> = mutex.withLock {
        aliases.size.toLong().right()
    }

    override suspend fun listAll(offset: Int, limit: Int): Either<PersistenceError, List<ScopeAlias>> = mutex.withLock {
        aliases.values
            .drop(offset)
            .take(limit)
            .toList()
            .right()
    }
}
