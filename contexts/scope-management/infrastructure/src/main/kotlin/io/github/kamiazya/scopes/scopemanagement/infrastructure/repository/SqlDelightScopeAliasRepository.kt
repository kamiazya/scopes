package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.db.ScopeManagementDatabase
import io.github.kamiazya.scopes.scopemanagement.db.Scope_aliases
import io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
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

    companion object {
        // SQLite has a limit of 999 variables in a single query
        private const val SQLITE_VARIABLE_LIMIT = 999
    }

    override suspend fun save(alias: ScopeAlias): Either<ScopesError, Unit> = try {
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
        when {
            // SQLite unique constraint violation detection
            // Check for constraint violation using multiple detection methods
            isSqliteUniqueConstraintViolation(e) -> {
                // Extract the existing scope ID that owns this alias
                val existingScopeId = try {
                    database.scopeAliasQueries.findByAliasName(alias.aliasName.value)
                        .executeAsOneOrNull()?.scope_id?.let { ScopeId.create(it) }
                        ?.fold(
                            ifLeft = { null },
                            ifRight = { it },
                        )
                } catch (_: Exception) {
                    null
                }

                if (existingScopeId != null) {
                    // Return business-specific duplicate alias error
                    ScopeAliasError.DuplicateAlias(
                        aliasName = alias.aliasName,
                        existingScopeId = existingScopeId,
                        attemptedScopeId = alias.scopeId,
                    ).left()
                } else {
                    // Fallback to repository error if we can't determine the existing scope
                    ScopesError.RepositoryError(
                        repositoryName = "SqlDelightScopeAliasRepository",
                        operation = ScopesError.RepositoryError.RepositoryOperation.SAVE,
                        entityType = "ScopeAlias",
                        entityId = alias.id.value,
                        failure = ScopesError.RepositoryError.RepositoryFailure.CONSTRAINT_VIOLATION,
                    ).left()
                }
            }
            else -> {
                // All other database errors
                ScopesError.RepositoryError(
                    repositoryName = "SqlDelightScopeAliasRepository",
                    operation = ScopesError.RepositoryError.RepositoryOperation.SAVE,
                    entityType = "ScopeAlias",
                    entityId = alias.id.value,
                    failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
                ).left()
            }
        }
    }

    override suspend fun findByAliasName(aliasName: AliasName): Either<ScopesError, ScopeAlias?> = try {
        val result = database.scopeAliasQueries.findByAliasName(aliasName.value).executeAsOneOrNull()
        result?.let { rowToScopeAlias(it) }.right()
    } catch (e: Exception) {
        ScopesError.RepositoryError(
            repositoryName = "SqlDelightScopeAliasRepository",
            operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
            entityType = "ScopeAlias",
            failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
        ).left()
    }

    override suspend fun findById(aliasId: AliasId): Either<ScopesError, ScopeAlias?> = try {
        val result = database.scopeAliasQueries.findById(aliasId.value).executeAsOneOrNull()
        result?.let { rowToScopeAlias(it) }.right()
    } catch (e: Exception) {
        ScopesError.RepositoryError(
            repositoryName = "SqlDelightScopeAliasRepository",
            operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
            entityType = "ScopeAlias",
            entityId = aliasId.value,
            failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
        ).left()
    }

    override suspend fun findByScopeId(scopeId: ScopeId): Either<ScopesError, List<ScopeAlias>> = try {
        val results = database.scopeAliasQueries.findByScopeId(scopeId.value).executeAsList()
        results.map { rowToScopeAlias(it) }.right()
    } catch (e: Exception) {
        ScopesError.RepositoryError(
            repositoryName = "SqlDelightScopeAliasRepository",
            operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
            entityType = "ScopeAlias",
            failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
        ).left()
    }

    override suspend fun findCanonicalByScopeId(scopeId: ScopeId): Either<ScopesError, ScopeAlias?> = try {
        val result = database.scopeAliasQueries.findCanonicalAlias(scopeId.value).executeAsOneOrNull()
        result?.let { rowToScopeAlias(it) }.right()
    } catch (e: Exception) {
        ScopesError.RepositoryError(
            repositoryName = "SqlDelightScopeAliasRepository",
            operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
            entityType = "ScopeAlias",
            failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
        ).left()
    }

    override suspend fun findCanonicalByScopeIds(scopeIds: List<ScopeId>): Either<ScopesError, List<ScopeAlias>> = try {
        if (scopeIds.isEmpty()) {
            emptyList<ScopeAlias>().right()
        } else {
            // SQLite has a limit on the number of variables in a single query
            // Chunk the IDs to avoid hitting this limit
            val scopeIdValues = scopeIds.map { it.value }

            val allResults = if (scopeIdValues.size <= SQLITE_VARIABLE_LIMIT) {
                // Single query for small lists
                database.scopeAliasQueries.findCanonicalAliasesBatch(scopeIdValues).executeAsList()
            } else {
                // Multiple queries for large lists
                scopeIdValues.chunked(SQLITE_VARIABLE_LIMIT).flatMap { chunk ->
                    database.scopeAliasQueries.findCanonicalAliasesBatch(chunk).executeAsList()
                }
            }

            allResults.map { rowToScopeAlias(it) }.right()
        }
    } catch (e: Exception) {
        ScopesError.RepositoryError(
            repositoryName = "SqlDelightScopeAliasRepository",
            operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
            entityType = "ScopeAlias",
            failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
        ).left()
    }

    override suspend fun findByScopeIdAndType(scopeId: ScopeId, aliasType: AliasType): Either<ScopesError, List<ScopeAlias>> = try {
        val results = database.scopeAliasQueries.findByTypeForScope(scopeId.value, aliasType.name).executeAsList()
        results.map { rowToScopeAlias(it) }.right()
    } catch (e: Exception) {
        ScopesError.RepositoryError(
            repositoryName = "SqlDelightScopeAliasRepository",
            operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
            entityType = "ScopeAlias",
            failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
        ).left()
    }

    override suspend fun findByAliasNamePrefix(prefix: String, limit: Int): Either<ScopesError, List<ScopeAlias>> = try {
        val results = database.scopeAliasQueries.findByPrefix("$prefix%", limit.toLong()).executeAsList()
        results.map { rowToScopeAlias(it) }.right()
    } catch (e: Exception) {
        ScopesError.RepositoryError(
            repositoryName = "SqlDelightScopeAliasRepository",
            operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
            entityType = "ScopeAlias",
            failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
        ).left()
    }

    override suspend fun existsByAliasName(aliasName: AliasName): Either<ScopesError, Boolean> = try {
        val result = database.scopeAliasQueries.existsByAliasName(aliasName.value).executeAsOne()
        result.right()
    } catch (e: Exception) {
        ScopesError.RepositoryError(
            repositoryName = "SqlDelightScopeAliasRepository",
            operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
            entityType = "ScopeAlias",
            failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
        ).left()
    }

    override suspend fun removeById(aliasId: AliasId): Either<ScopesError, Boolean> = try {
        database.scopeAliasQueries.deleteById(aliasId.value)
        true.right()
    } catch (e: Exception) {
        ScopesError.RepositoryError(
            repositoryName = "SqlDelightScopeAliasRepository",
            operation = ScopesError.RepositoryError.RepositoryOperation.DELETE,
            entityType = "ScopeAlias",
            entityId = aliasId.value,
            failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
        ).left()
    }

    override suspend fun removeByAliasName(aliasName: AliasName): Either<ScopesError, Boolean> = try {
        database.scopeAliasQueries.deleteByAliasName(aliasName.value)
        true.right()
    } catch (e: Exception) {
        ScopesError.RepositoryError(
            repositoryName = "SqlDelightScopeAliasRepository",
            operation = ScopesError.RepositoryError.RepositoryOperation.DELETE,
            entityType = "ScopeAlias",
            failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
        ).left()
    }

    override suspend fun removeByScopeId(scopeId: ScopeId): Either<ScopesError, Int> = try {
        database.scopeAliasQueries.deleteAllForScope(scopeId.value)
        0.right() // SQLDelight doesn't return row count
    } catch (e: Exception) {
        ScopesError.RepositoryError(
            repositoryName = "SqlDelightScopeAliasRepository",
            operation = ScopesError.RepositoryError.RepositoryOperation.DELETE,
            entityType = "ScopeAlias",
            failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
        ).left()
    }

    override suspend fun update(alias: ScopeAlias): Either<ScopesError, Boolean> = try {
        database.scopeAliasQueries.updateAlias(
            scope_id = alias.scopeId.value,
            alias_name = alias.aliasName.value,
            alias_type = alias.aliasType.name,
            updated_at = alias.updatedAt.toEpochMilliseconds(),
            id = alias.id.value,
        )
        true.right()
    } catch (e: Exception) {
        ScopesError.RepositoryError(
            repositoryName = "SqlDelightScopeAliasRepository",
            operation = ScopesError.RepositoryError.RepositoryOperation.UPDATE,
            entityType = "ScopeAlias",
            entityId = alias.id.value,
            failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
        ).left()
    }

    override suspend fun count(): Either<ScopesError, Long> = try {
        val result = database.scopeAliasQueries.countAliases().executeAsOne()
        result.right()
    } catch (e: Exception) {
        ScopesError.RepositoryError(
            repositoryName = "SqlDelightScopeAliasRepository",
            operation = ScopesError.RepositoryError.RepositoryOperation.COUNT,
            entityType = "ScopeAlias",
            failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
        ).left()
    }

    override suspend fun listAll(offset: Int, limit: Int): Either<ScopesError, List<ScopeAlias>> = try {
        val results = database.scopeAliasQueries.getAllAliasesPaged(limit.toLong(), offset.toLong()).executeAsList()
        results.map { rowToScopeAlias(it) }.right()
    } catch (e: Exception) {
        ScopesError.RepositoryError(
            repositoryName = "SqlDelightScopeAliasRepository",
            operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
            entityType = "ScopeAlias",
            failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
        ).left()
    }

    private fun rowToScopeAlias(row: Scope_aliases): ScopeAlias = ScopeAlias(
        _id = AliasId.create(row.id).fold(
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

    /**
     * Checks if the given exception represents a SQLite unique constraint violation.
     *
     * This method uses multiple detection strategies to identify constraint violations:
     * 1. Checks for specific error messages that SQLite returns
     * 2. Checks for SQLite error codes if available (e.g., SQLITE_CONSTRAINT = 19)
     * 3. Handles variations across different SQLite driver versions
     *
     * @param e The exception to check
     * @return true if this is a unique constraint violation, false otherwise
     */
    private fun isSqliteUniqueConstraintViolation(e: Exception): Boolean {
        val message = e.message ?: return false

        // Check for various SQLite unique constraint error patterns
        return message.contains("UNIQUE constraint failed", ignoreCase = true) ||
            message.contains("SQLITE_CONSTRAINT_UNIQUE", ignoreCase = true) ||
            message.contains("constraint failed", ignoreCase = true) &&
            message.contains("unique", ignoreCase = true) ||
            // Some drivers may include the error code directly
            message.contains("error code 19", ignoreCase = true) ||
            message.contains("SQLITE_CONSTRAINT", ignoreCase = true) ||
            // Check the cause chain for nested constraint violations
            (e.cause?.message?.let { isSqliteUniqueConstraintViolation(Exception(it)) } ?: false)
    }
}
