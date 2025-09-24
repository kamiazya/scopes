package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.Either
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
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

    override suspend fun save(alias: ScopeAlias): Either<ScopesError, Unit> = mutex.withLock {
        aliases[alias.id] = alias
        aliasNameIndex[alias.aliasName.value] = alias.id
        scopeIdIndex.getOrPut(alias.scopeId) { mutableSetOf() }.add(alias.id)
        Unit.right()
    }

    override suspend fun findByAliasName(aliasName: AliasName): Either<ScopesError, ScopeAlias?> = mutex.withLock {
        val aliasId = aliasNameIndex[aliasName.value]
        val alias = aliasId?.let { aliases[it] }
        alias.right()
    }

    override suspend fun findById(aliasId: AliasId): Either<ScopesError, ScopeAlias?> = mutex.withLock {
        aliases[aliasId].right()
    }

    override suspend fun findByScopeId(scopeId: ScopeId): Either<ScopesError, List<ScopeAlias>> = mutex.withLock {
        val aliasIds = scopeIdIndex[scopeId] ?: emptySet()
        val aliasList = aliasIds.mapNotNull { aliases[it] }
        aliasList.right()
    }

    override suspend fun findCanonicalByScopeId(scopeId: ScopeId): Either<ScopesError, ScopeAlias?> = mutex.withLock {
        val aliasIds = scopeIdIndex[scopeId] ?: emptySet()
        val canonicalAlias = aliasIds
            .mapNotNull { aliases[it] }
            .find { it.aliasType == AliasType.CANONICAL }
        canonicalAlias.right()
    }

    override suspend fun findCanonicalByScopeIds(scopeIds: List<ScopeId>): Either<ScopesError, List<ScopeAlias>> = mutex.withLock {
        val canonicalAliases = scopeIds.mapNotNull { scopeId ->
            val aliasIds = scopeIdIndex[scopeId] ?: emptySet()
            aliasIds
                .mapNotNull { aliases[it] }
                .find { it.aliasType == AliasType.CANONICAL }
        }
        canonicalAliases.right()
    }

    override suspend fun findByScopeIdAndType(scopeId: ScopeId, aliasType: AliasType): Either<ScopesError, List<ScopeAlias>> = mutex.withLock {
        val aliasIds = scopeIdIndex[scopeId] ?: emptySet()
        val filteredAliases = aliasIds
            .mapNotNull { aliases[it] }
            .filter { it.aliasType == aliasType }
        filteredAliases.right()
    }

    override suspend fun findByAliasNamePrefix(prefix: String, limit: Int): Either<ScopesError, List<ScopeAlias>> = mutex.withLock {
        val matchingAliases = aliasNameIndex.keys
            .filter { it.startsWith(prefix) }
            .take(limit)
            .mapNotNull { name -> aliasNameIndex[name]?.let { aliases[it] } }
        matchingAliases.right()
    }

    override suspend fun existsByAliasName(aliasName: AliasName): Either<ScopesError, Boolean> = mutex.withLock {
        aliasNameIndex.containsKey(aliasName.value).right()
    }

    override suspend fun removeById(aliasId: AliasId): Either<ScopesError, Boolean> = mutex.withLock {
        val alias = aliases[aliasId]
        if (alias != null) {
            aliases.remove(aliasId)
            aliasNameIndex.remove(alias.aliasName.value)
            scopeIdIndex[alias.scopeId]?.remove(aliasId)
            true.right()
        } else {
            false.right()
        }
    }

    override suspend fun removeByAliasName(aliasName: AliasName): Either<ScopesError, Boolean> = mutex.withLock {
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

    override suspend fun removeByScopeId(scopeId: ScopeId): Either<ScopesError, Int> = mutex.withLock {
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

    override suspend fun update(alias: ScopeAlias): Either<ScopesError, Boolean> = mutex.withLock {
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

    override suspend fun count(): Either<ScopesError, Long> = mutex.withLock {
        aliases.size.toLong().right()
    }

    override suspend fun listAll(offset: Int, limit: Int): Either<ScopesError, List<ScopeAlias>> = mutex.withLock {
        aliases.values
            .drop(offset)
            .take(limit)
            .toList()
            .right()
    }

    // Event projection methods

    override suspend fun save(aliasId: AliasId, aliasName: AliasName, scopeId: ScopeId, aliasType: AliasType): Either<ScopesError, Unit> = mutex.withLock {
        val alias = ScopeAlias(
            id = aliasId,
            scopeId = scopeId,
            aliasName = aliasName,
            aliasType = aliasType,
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now(),
        )
        save(alias)
    }

    override suspend fun updateAliasName(aliasId: AliasId, newAliasName: AliasName): Either<ScopesError, Unit> = mutex.withLock {
        val existing = aliases[aliasId]
        if (existing != null) {
            val updated = existing.copy(
                aliasName = newAliasName,
                updatedAt = kotlinx.datetime.Clock.System.now(),
            )
            save(updated)
        } else {
            Unit.right()
        }
    }

    override suspend fun updateAliasType(aliasId: AliasId, newAliasType: AliasType): Either<ScopesError, Unit> = mutex.withLock {
        val existing = aliases[aliasId]
        if (existing != null) {
            val updated = existing.copy(
                aliasType = newAliasType,
                updatedAt = kotlinx.datetime.Clock.System.now(),
            )
            save(updated)
        } else {
            Unit.right()
        }
    }

    override suspend fun deleteById(aliasId: AliasId): Either<ScopesError, Unit> = mutex.withLock {
        removeById(aliasId).map { }
    }
}
