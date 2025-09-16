package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.db.ScopeManagementDatabase
import io.github.kamiazya.scopes.scopemanagement.db.Scope_aliases
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Instant

/**
 * SQLDelight implementation of ScopeAliasRepository.
 */
class SqlDelightScopeAliasRepository(private val database: ScopeManagementDatabase) : ScopeAliasRepository {

    override suspend fun save(alias: ScopeAlias): Either<PersistenceError, Unit> = try {
        val existing = database.scopeAliasQueries.findById(alias.id.value).executeAsOneOrNull()

        if (existing != null) {
            database.scopeAliasQueries.updateAlias(
                scope_id = alias.scopeId.value,
                alias_name = alias.aliasName.value,
                alias_type = alias.aliasType.name,
                updated_at = alias.updatedAt.toEpochMilliseconds(),
                id = alias.id.value,
            )
        } else {
            database.scopeAliasQueries.insertAlias(
                id = alias.id.value,
                scope_id = alias.scopeId.value,
                alias_name = alias.aliasName.value,
                alias_type = alias.aliasType.name,
                created_at = alias.createdAt.toEpochMilliseconds(),
                updated_at = alias.updatedAt.toEpochMilliseconds(),
            )
        }
        Unit.right()
    } catch (e: Exception) {
        PersistenceError.StorageUnavailable(
            operation = "save",
            cause = e,
        ).left()
    }

    override suspend fun findByAliasName(aliasName: AliasName): Either<PersistenceError, ScopeAlias?> = try {
        val result = database.scopeAliasQueries.findByAliasName(aliasName.value).executeAsOneOrNull()
        result?.let { rowToScopeAlias(it) }.right()
    } catch (e: Exception) {
        PersistenceError.StorageUnavailable(
            operation = "findByAliasName",
            cause = e,
        ).left()
    }

    override suspend fun findById(aliasId: AliasId): Either<PersistenceError, ScopeAlias?> = try {
        val result = database.scopeAliasQueries.findById(aliasId.value).executeAsOneOrNull()
        result?.let { rowToScopeAlias(it) }.right()
    } catch (e: Exception) {
        PersistenceError.StorageUnavailable(
            operation = "findById",
            cause = e,
        ).left()
    }

    override suspend fun findByScopeId(scopeId: ScopeId): Either<PersistenceError, List<ScopeAlias>> = try {
        val results = database.scopeAliasQueries.findByScopeId(scopeId.value).executeAsList()
        results.map { rowToScopeAlias(it) }.right()
    } catch (e: Exception) {
        PersistenceError.StorageUnavailable(
            operation = "findByScopeId",
            cause = e,
        ).left()
    }

    override suspend fun findCanonicalByScopeId(scopeId: ScopeId): Either<PersistenceError, ScopeAlias?> = try {
        val result = database.scopeAliasQueries.findCanonicalAlias(scopeId.value).executeAsOneOrNull()
        result?.let { rowToScopeAlias(it) }.right()
    } catch (e: Exception) {
        PersistenceError.StorageUnavailable(
            operation = "findCanonicalByScopeId",
            cause = e,
        ).left()
    }

    override suspend fun findByScopeIdAndType(scopeId: ScopeId, aliasType: AliasType): Either<PersistenceError, List<ScopeAlias>> = try {
        val results = database.scopeAliasQueries.findByTypeForScope(scopeId.value, aliasType.name).executeAsList()
        results.map { rowToScopeAlias(it) }.right()
    } catch (e: Exception) {
        PersistenceError.StorageUnavailable(
            operation = "findByScopeIdAndType",
            cause = e,
        ).left()
    }

    override suspend fun findByAliasNamePrefix(prefix: String, limit: Int): Either<PersistenceError, List<ScopeAlias>> = try {
        val results = database.scopeAliasQueries.findByPrefix("$prefix%", limit.toLong()).executeAsList()
        results.map { rowToScopeAlias(it) }.right()
    } catch (e: Exception) {
        PersistenceError.StorageUnavailable(
            operation = "findByAliasNamePrefix",
            cause = e,
        ).left()
    }

    override suspend fun existsByAliasName(aliasName: AliasName): Either<PersistenceError, Boolean> = try {
        val result = database.scopeAliasQueries.existsByAliasName(aliasName.value).executeAsOne()
        result.right()
    } catch (e: Exception) {
        PersistenceError.StorageUnavailable(
            operation = "existsByAliasName",
            cause = e,
        ).left()
    }

    override suspend fun removeById(aliasId: AliasId): Either<PersistenceError, Boolean> = try {
        database.scopeAliasQueries.deleteById(aliasId.value)
        true.right()
    } catch (e: Exception) {
        PersistenceError.StorageUnavailable(
            operation = "removeById",
            cause = e,
        ).left()
    }

    override suspend fun removeByAliasName(aliasName: AliasName): Either<PersistenceError, Boolean> = try {
        database.scopeAliasQueries.deleteByAliasName(aliasName.value)
        true.right()
    } catch (e: Exception) {
        PersistenceError.StorageUnavailable(
            operation = "removeByAliasName",
            cause = e,
        ).left()
    }

    override suspend fun removeByScopeId(scopeId: ScopeId): Either<PersistenceError, Int> = try {
        database.scopeAliasQueries.deleteAllForScope(scopeId.value)
        0.right() // SQLDelight doesn't return row count
    } catch (e: Exception) {
        PersistenceError.StorageUnavailable(
            operation = "removeByScopeId",
            cause = e,
        ).left()
    }

    override suspend fun update(alias: ScopeAlias): Either<PersistenceError, Boolean> = try {
        database.scopeAliasQueries.updateAlias(
            scope_id = alias.scopeId.value,
            alias_name = alias.aliasName.value,
            alias_type = alias.aliasType.name,
            updated_at = alias.updatedAt.toEpochMilliseconds(),
            id = alias.id.value,
        )
        true.right()
    } catch (e: Exception) {
        PersistenceError.StorageUnavailable(
            operation = "update",
            cause = e,
        ).left()
    }

    override suspend fun count(): Either<PersistenceError, Long> = try {
        val result = database.scopeAliasQueries.countAliases().executeAsOne()
        result.right()
    } catch (e: Exception) {
        PersistenceError.StorageUnavailable(
            operation = "count",
            cause = e,
        ).left()
    }

    override suspend fun listAll(offset: Int, limit: Int): Either<PersistenceError, List<ScopeAlias>> = try {
        val results = database.scopeAliasQueries.getAllAliasesPaged(limit.toLong(), offset.toLong()).executeAsList()
        results.map { rowToScopeAlias(it) }.right()
    } catch (e: Exception) {
        PersistenceError.StorageUnavailable(
            operation = "listAll",
            cause = e,
        ).left()
    }

    private fun rowToScopeAlias(row: Scope_aliases): ScopeAlias = ScopeAlias(
        id = AliasId.create(row.id).fold(
            ifLeft = { error("Invalid alias id in database: $it") },
            ifRight = { it },
        ),
        scopeId = ScopeId.create(row.scope_id).fold(
            ifLeft = { error("Invalid scope id in database: $it") },
            ifRight = { it },
        ),
        aliasName = AliasName.create(row.alias_name).fold(
            ifLeft = { error("Invalid alias name in database: $it") },
            ifRight = { it },
        ),
        aliasType = AliasType.valueOf(row.alias_type),
        createdAt = Instant.fromEpochMilliseconds(row.created_at),
        updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
    )
}
