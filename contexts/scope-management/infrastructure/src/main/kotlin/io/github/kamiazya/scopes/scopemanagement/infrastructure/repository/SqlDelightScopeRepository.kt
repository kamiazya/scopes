package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import io.github.kamiazya.scopes.scopemanagement.db.ScopeManagementDatabase
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Aspects
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

/**
 * SQLDelight implementation of ScopeRepository.
 */
class SqlDelightScopeRepository(private val database: ScopeManagementDatabase) : ScopeRepository {
    companion object {
        // SQLite has a default limit of 999 variables in a single query
        private const val SQLITE_VARIABLE_LIMIT = 999
    }

    override suspend fun save(scope: Scope): Either<ScopesError, Scope> = withContext(Dispatchers.IO) {
        try {
            database.transaction {
                // Use UPSERT for atomic operation
                database.scopeQueries.upsertScope(
                    id = scope.id.value,
                    title = scope.title.value,
                    description = scope.description?.value,
                    parent_id = scope.parentId?.value,
                    created_at = scope.createdAt.toEpochMilliseconds(),
                    updated_at = scope.updatedAt.toEpochMilliseconds(),
                )

                // Delete existing aspects
                database.scopeAspectQueries.deleteAllForScope(scope.id.value)

                // Insert new aspects
                scope.aspects.toMap().forEach { (key, values) ->
                    values.forEach { value ->
                        database.scopeAspectQueries.insertAspect(
                            scope_id = scope.id.value,
                            aspect_key = key.value,
                            aspect_value = value.value,
                        )
                    }
                }
            }

            scope.right()
        } catch (e: Exception) {
            ScopesError.RepositoryError(
                repositoryName = "SqlDelightScopeRepository",
                operation = ScopesError.RepositoryError.RepositoryOperation.SAVE,
                entityType = "Scope",
                entityId = scope.id.value,
                failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
            ).left()
        }
    }

    override suspend fun findById(id: ScopeId): Either<ScopesError, Scope?> = withContext(Dispatchers.IO) {
        try {
            val scopeRow = database.scopeQueries.findScopeById(id.value).executeAsOneOrNull()
            scopeRow?.let { rowToScope(it) }.right()
        } catch (e: Exception) {
            ScopesError.RepositoryError(
                repositoryName = "SqlDelightScopeRepository",
                operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
                entityType = "Scope",
                entityId = id.value,
                failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
            ).left()
        }
    }

    override suspend fun findAll(): Either<ScopesError, List<Scope>> = withContext(Dispatchers.IO) {
        try {
            val scopeRows = database.scopeQueries.selectAll().executeAsList()
            if (scopeRows.isEmpty()) {
                return@withContext emptyList<Scope>().right()
            }

            // Batch load all aspects to avoid N+1 queries
            val scopeIds = scopeRows.map { it.id }
            val aspectsMap = loadAspectsForScopes(scopeIds)

            val scopes = scopeRows.map { row ->
                rowToScopeWithAspects(row, aspectsMap[row.id] ?: emptyList())
            }
            scopes.right()
        } catch (e: Exception) {
            ScopesError.RepositoryError(
                repositoryName = "SqlDelightScopeRepository",
                operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
                entityType = "Scope",
                failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
            ).left()
        }
    }

    override suspend fun findByParentId(parentId: ScopeId?, offset: Int, limit: Int): Either<ScopesError, List<Scope>> = withContext(Dispatchers.IO) {
        try {
            val rows = if (parentId != null) {
                database.scopeQueries.findScopesByParentIdPaged(parentId.value, limit.toLong(), offset.toLong()).executeAsList()
            } else {
                database.scopeQueries.findRootScopesPaged(limit.toLong(), offset.toLong()).executeAsList()
            }

            if (rows.isEmpty()) {
                emptyList<Scope>().right()
            } else {
                val scopeIds = rows.map { it.id }
                val aspectsMap = loadAspectsForScopes(scopeIds)
                rows.map { row -> rowToScopeWithAspects(row, aspectsMap[row.id] ?: emptyList()) }.right()
            }
        } catch (e: Exception) {
            ScopesError.RepositoryError(
                repositoryName = "SqlDelightScopeRepository",
                operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
                entityType = "Scope",
                failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
            ).left()
        }
    }

    override suspend fun existsById(id: ScopeId): Either<ScopesError, Boolean> = withContext(Dispatchers.IO) {
        try {
            val exists = database.scopeQueries.existsById(id.value).executeAsOne()
            exists.right()
        } catch (e: Exception) {
            ScopesError.RepositoryError(
                repositoryName = "SqlDelightScopeRepository",
                operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
                entityType = "Scope",
                entityId = id.value,
                failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
            ).left()
        }
    }

    override suspend fun existsByParentIdAndTitle(parentId: ScopeId?, title: String): Either<ScopesError, Boolean> = withContext(Dispatchers.IO) {
        try {
            val exists = if (parentId != null) {
                database.scopeQueries.existsByTitleAndParent(title, parentId.value).executeAsOne()
            } else {
                database.scopeQueries.existsByTitleRoot(title).executeAsOne()
            }

            exists.right()
        } catch (e: Exception) {
            ScopesError.RepositoryError(
                repositoryName = "SqlDelightScopeRepository",
                operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
                entityType = "Scope",
                failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
            ).left()
        }
    }

    override suspend fun findIdByParentIdAndTitle(parentId: ScopeId?, title: String): Either<ScopesError, ScopeId?> = withContext(Dispatchers.IO) {
        try {
            val id = if (parentId != null) {
                database.scopeQueries.findScopeIdByTitleAndParent(title, parentId.value).executeAsOneOrNull()
            } else {
                database.scopeQueries.findScopeIdByTitleRoot(title).executeAsOneOrNull()
            }

            id?.let {
                ScopeId.create(it).fold(
                    ifLeft = { error("Invalid scope id in database: $it") },
                    ifRight = { it },
                )
            }.right()
        } catch (e: Exception) {
            ScopesError.RepositoryError(
                repositoryName = "SqlDelightScopeRepository",
                operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
                entityType = "Scope",
                failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
            ).left()
        }
    }

    override suspend fun deleteById(id: ScopeId): Either<ScopesError, Unit> = withContext(Dispatchers.IO) {
        try {
            database.transaction {
                // Delete aspects first
                database.scopeAspectQueries.deleteAllForScope(id.value)

                // Delete the scope
                database.scopeQueries.deleteScope(id.value)
            }

            Unit.right()
        } catch (e: Exception) {
            ScopesError.RepositoryError(
                repositoryName = "SqlDelightScopeRepository",
                operation = ScopesError.RepositoryError.RepositoryOperation.DELETE,
                entityType = "Scope",
                entityId = id.value,
                failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
            ).left()
        }
    }

    override suspend fun update(scope: Scope): Either<ScopesError, Scope> = save(scope)

    override suspend fun countChildrenOf(parentId: ScopeId): Either<ScopesError, Int> = withContext(Dispatchers.IO) {
        try {
            val count = database.scopeQueries.countChildren(parentId.value).executeAsOne()
            count.toInt().right()
        } catch (e: Exception) {
            ScopesError.RepositoryError(
                repositoryName = "SqlDelightScopeRepository",
                operation = ScopesError.RepositoryError.RepositoryOperation.COUNT,
                entityType = "Scope",
                failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
            ).left()
        }
    }

    override suspend fun countByParentId(parentId: ScopeId?): Either<ScopesError, Int> = withContext(Dispatchers.IO) {
        try {
            val count = if (parentId != null) {
                database.scopeQueries.countScopesByParentId(parentId.value).executeAsOne()
            } else {
                database.scopeQueries.countRootScopes().executeAsOne()
            }
            count.toInt().right()
        } catch (e: Exception) {
            ScopesError.RepositoryError(
                repositoryName = "SqlDelightScopeRepository",
                operation = ScopesError.RepositoryError.RepositoryOperation.COUNT,
                entityType = "Scope",
                failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
            ).left()
        }
    }

    override suspend fun findAll(offset: Int, limit: Int): Either<ScopesError, List<Scope>> = withContext(Dispatchers.IO) {
        try {
            val rows = database.scopeQueries.selectAllPaged(limit.toLong(), offset.toLong())
                .executeAsList()

            if (rows.isEmpty()) {
                emptyList<Scope>().right()
            } else {
                val scopeIds = rows.map { it.id }
                val aspectsMap = loadAspectsForScopes(scopeIds)
                rows.map { row -> rowToScopeWithAspects(row, aspectsMap[row.id] ?: emptyList()) }.right()
            }
        } catch (e: Exception) {
            ScopesError.RepositoryError(
                repositoryName = "SqlDelightScopeRepository",
                operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
                entityType = "Scope",
                failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
            ).left()
        }
    }

    override suspend fun findAllRoot(): Either<ScopesError, List<Scope>> = withContext(Dispatchers.IO) {
        try {
            val rows = database.scopeQueries.findRootScopes()
                .executeAsList()
                .map { row -> rowToScope(row) }
            rows.right()
        } catch (e: Exception) {
            ScopesError.RepositoryError(
                repositoryName = "SqlDelightScopeRepository",
                operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
                entityType = "Scope",
                failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
            ).left()
        }
    }

    private fun rowToScope(row: io.github.kamiazya.scopes.scopemanagement.db.Scopes): Scope {
        val scopeId = ScopeId.create(row.id).fold(
            ifLeft = { error("Invalid scope id in database: $it") },
            ifRight = { it },
        )

        // Load aspects
        val aspectRows = database.scopeAspectQueries.findByScopeId(scopeId.value).executeAsList()

        val aspectMap = aspectRows
            .groupBy {
                AspectKey.create(it.aspect_key).fold(
                    ifLeft = { error("Invalid aspect key in database: $it") },
                    ifRight = { it },
                )
            }
            .mapValues { (_, rows) ->
                rows.map { aspectRow ->
                    AspectValue.create(aspectRow.aspect_value).fold(
                        ifLeft = { error("Invalid aspect value in database: $it") },
                        ifRight = { it },
                    )
                }
                    .toNonEmptyListOrNull() ?: error(
                    "Aspect key exists without values in database - data integrity violation",
                )
            }

        return Scope(
            _id = scopeId,
            title = ScopeTitle.create(row.title).fold(
                ifLeft = { error("Invalid title in database: $it") },
                ifRight = { it },
            ),
            description = row.description?.let { desc ->
                ScopeDescription.create(desc).fold(
                    ifLeft = { error("Invalid description in database: $it") },
                    ifRight = { it },
                )
            },
            parentId = row.parent_id?.let { pid ->
                ScopeId.create(pid).fold(
                    ifLeft = { error("Invalid parent id in database: $it") },
                    ifRight = { it },
                )
            },
            aspects = if (aspectMap.isEmpty()) Aspects.empty() else Aspects.from(aspectMap),
            createdAt = Instant.fromEpochMilliseconds(row.created_at),
            updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
        )
    }

    private fun loadAspectsForScopes(scopeIds: List<String>): Map<String, List<io.github.kamiazya.scopes.scopemanagement.db.FindByScopeIds>> {
        if (scopeIds.isEmpty()) return emptyMap()

        // SQLite has a limit on the number of variables in a single query,
        // so we need to chunk large lists
        return if (scopeIds.size <= SQLITE_VARIABLE_LIMIT) {
            database.scopeAspectQueries.findByScopeIds(scopeIds)
                .executeAsList()
                .groupBy { it.scope_id }
        } else {
            scopeIds.chunked(SQLITE_VARIABLE_LIMIT).flatMap { chunk ->
                database.scopeAspectQueries.findByScopeIds(chunk).executeAsList()
            }.groupBy { it.scope_id }
        }
    }

    private fun rowToScopeWithAspects(
        row: io.github.kamiazya.scopes.scopemanagement.db.Scopes,
        aspectRows: List<io.github.kamiazya.scopes.scopemanagement.db.FindByScopeIds>,
    ): Scope {
        val scopeId = ScopeId.create(row.id).fold(
            ifLeft = { error("Invalid scope id in database: $it") },
            ifRight = { it },
        )

        val aspectMap = aspectRows
            .groupBy {
                AspectKey.create(it.aspect_key).fold(
                    ifLeft = { error("Invalid aspect key in database: $it") },
                    ifRight = { it },
                )
            }
            .mapValues { (_, rows) ->
                rows.map { aspectRow ->
                    AspectValue.create(aspectRow.aspect_value).fold(
                        ifLeft = { error("Invalid aspect value in database: $it") },
                        ifRight = { it },
                    )
                }
                    .toNonEmptyListOrNull() ?: error(
                    "Aspect key exists without values in database - data integrity violation",
                )
            }

        return Scope(
            _id = scopeId,
            title = ScopeTitle.create(row.title).fold(
                ifLeft = { error("Invalid title in database: $it") },
                ifRight = { it },
            ),
            description = row.description?.let { desc ->
                ScopeDescription.create(desc).fold(
                    ifLeft = { error("Invalid description in database: $it") },
                    ifRight = { it },
                )
            },
            parentId = row.parent_id?.let { pid ->
                ScopeId.create(pid).fold(
                    ifLeft = { error("Invalid parent id in database: $it") },
                    ifRight = { it },
                )
            },
            aspects = if (aspectMap.isEmpty()) Aspects.empty() else Aspects.from(aspectMap),
            createdAt = Instant.fromEpochMilliseconds(row.created_at),
            updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
        )
    }

    override suspend fun countByAspectKey(aspectKey: AspectKey): Either<ScopesError, Int> = withContext(Dispatchers.IO) {
        try {
            val count = database.scopeAspectQueries.countByAspectKey(aspectKey.value).executeAsOne()
            count.toInt().right()
        } catch (e: Exception) {
            ScopesError.RepositoryError(
                repositoryName = "SqlDelightScopeRepository",
                operation = ScopesError.RepositoryError.RepositoryOperation.COUNT,
                entityType = "Scope",
                failure = ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
            ).left()
        }
    }
}
