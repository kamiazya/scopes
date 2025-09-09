package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import arrow.core.raise.either
import arrow.core.flatMap
import io.github.kamiazya.scopes.scopemanagement.db.ScopeManagementDatabase
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Aspects
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeStatus
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * SQLDelight implementation of ScopeRepository.
 */
class SqlDelightScopeRepository(private val database: ScopeManagementDatabase) : ScopeRepository {
    companion object {
        // SQLite has a default limit of 999 variables in a single query
        private const val SQLITE_VARIABLE_LIMIT = 999
    }

    override suspend fun save(scope: Scope): Either<PersistenceError, Scope> = withContext(Dispatchers.IO) {
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
            PersistenceError.StorageUnavailable(
                occurredAt = Clock.System.now(),
                operation = "save",
                cause = e,
            ).left()
        }
    }

    override suspend fun findById(id: ScopeId): Either<PersistenceError, Scope?> = withContext(Dispatchers.IO) {
        either {
            try {
                val scopeRow = database.scopeQueries.findScopeById(id.value).executeAsOneOrNull()
                scopeRow?.let { row -> 
                    rowToScope(row).bind()
                }
            } catch (e: Exception) {
                raise(
                    PersistenceError.StorageUnavailable(
                        occurredAt = Clock.System.now(),
                        operation = "findById",
                        cause = e,
                    )
                )
            }
        }
    }

    override suspend fun findAll(): Either<PersistenceError, List<Scope>> = withContext(Dispatchers.IO) {
        either {
            try {
                val scopeRows = database.scopeQueries.selectAll().executeAsList()
                if (scopeRows.isEmpty()) {
                    emptyList()
                } else {
                    // Batch load all aspects to avoid N+1 queries
                    val scopeIds = scopeRows.map { it.id }
                    val aspectsMap = loadAspectsForScopes(scopeIds)

                    scopeRows.map { row ->
                        rowToScopeWithAspects(row, aspectsMap[row.id] ?: emptyList()).bind()
                    }
                }
            } catch (e: Exception) {
                raise(
                    PersistenceError.StorageUnavailable(
                        occurredAt = Clock.System.now(),
                        operation = "findAll",
                        cause = e,
                    )
                )
            }
        }
    }

    override suspend fun findByParentId(parentId: ScopeId?, offset: Int, limit: Int): Either<PersistenceError, List<Scope>> = withContext(Dispatchers.IO) {
        either {
            try {
                val rows = if (parentId != null) {
                    database.scopeQueries.findScopesByParentIdPaged(parentId.value, limit.toLong(), offset.toLong()).executeAsList()
                } else {
                    database.scopeQueries.findRootScopesPaged(limit.toLong(), offset.toLong()).executeAsList()
                }

                if (rows.isEmpty()) {
                    emptyList()
                } else {
                    val scopeIds = rows.map { it.id }
                    val aspectsMap = loadAspectsForScopes(scopeIds)
                    rows.map { row -> 
                        rowToScopeWithAspects(row, aspectsMap[row.id] ?: emptyList()).bind()
                    }
                }
            } catch (e: Exception) {
                raise(
                    PersistenceError.StorageUnavailable(
                        occurredAt = Clock.System.now(),
                        operation = "findByParentId(offset,limit)",
                        cause = e,
                    )
                )
            }
        }
    }

    override suspend fun existsById(id: ScopeId): Either<PersistenceError, Boolean> = withContext(Dispatchers.IO) {
        try {
            val exists = database.scopeQueries.existsById(id.value).executeAsOne()
            exists.right()
        } catch (e: Exception) {
            PersistenceError.StorageUnavailable(
                occurredAt = Clock.System.now(),
                operation = "existsById",
                cause = e,
            ).left()
        }
    }

    override suspend fun existsByParentIdAndTitle(parentId: ScopeId?, title: String): Either<PersistenceError, Boolean> = withContext(Dispatchers.IO) {
        try {
            val exists = if (parentId != null) {
                database.scopeQueries.existsByTitleAndParent(title, parentId.value).executeAsOne()
            } else {
                database.scopeQueries.existsByTitleRoot(title).executeAsOne()
            }

            exists.right()
        } catch (e: Exception) {
            PersistenceError.StorageUnavailable(
                occurredAt = Clock.System.now(),
                operation = "existsByParentIdAndTitle",
                cause = e,
            ).left()
        }
    }

    override suspend fun findIdByParentIdAndTitle(parentId: ScopeId?, title: String): Either<PersistenceError, ScopeId?> = withContext(Dispatchers.IO) {
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
            PersistenceError.StorageUnavailable(
                occurredAt = Clock.System.now(),
                operation = "findIdByParentIdAndTitle",
                cause = e,
            ).left()
        }
    }

    override suspend fun deleteById(id: ScopeId): Either<PersistenceError, Unit> = withContext(Dispatchers.IO) {
        try {
            database.transaction {
                // Delete aspects first
                database.scopeAspectQueries.deleteAllForScope(id.value)

                // Delete the scope
                database.scopeQueries.deleteScope(id.value)
            }

            Unit.right()
        } catch (e: Exception) {
            PersistenceError.StorageUnavailable(
                occurredAt = Clock.System.now(),
                operation = "deleteById",
                cause = e,
            ).left()
        }
    }

    override suspend fun update(scope: Scope): Either<PersistenceError, Scope> = save(scope)

    override suspend fun countChildrenOf(parentId: ScopeId): Either<PersistenceError, Int> = withContext(Dispatchers.IO) {
        try {
            val count = database.scopeQueries.countChildren(parentId.value).executeAsOne()
            count.toInt().right()
        } catch (e: Exception) {
            PersistenceError.StorageUnavailable(
                occurredAt = Clock.System.now(),
                operation = "countChildrenOf",
                cause = e,
            ).left()
        }
    }

    override suspend fun countByParentId(parentId: ScopeId?): Either<PersistenceError, Int> = withContext(Dispatchers.IO) {
        try {
            val count = if (parentId != null) {
                database.scopeQueries.countScopesByParentId(parentId.value).executeAsOne()
            } else {
                database.scopeQueries.countRootScopes().executeAsOne()
            }
            count.toInt().right()
        } catch (e: Exception) {
            PersistenceError.StorageUnavailable(
                occurredAt = Clock.System.now(),
                operation = "countByParentId",
                cause = e,
            ).left()
        }
    }

    override suspend fun findAll(offset: Int, limit: Int): Either<PersistenceError, List<Scope>> = withContext(Dispatchers.IO) {
        either {
            try {
                val rows = database.scopeQueries.selectAllPaged(limit.toLong(), offset.toLong())
                    .executeAsList()

                if (rows.isEmpty()) {
                    emptyList()
                } else {
                    val scopeIds = rows.map { it.id }
                    val aspectsMap = loadAspectsForScopes(scopeIds)
                    rows.map { row -> 
                        rowToScopeWithAspects(row, aspectsMap[row.id] ?: emptyList()).bind()
                    }
                }
            } catch (e: Exception) {
                raise(
                    PersistenceError.StorageUnavailable(
                        occurredAt = Clock.System.now(),
                        operation = "findAll",
                        cause = e,
                    )
                )
            }
        }
    }

    override suspend fun findAllRoot(): Either<PersistenceError, List<Scope>> = withContext(Dispatchers.IO) {
        either {
            try {
                database.scopeQueries.findRootScopes()
                    .executeAsList()
                    .map { row -> rowToScope(row).bind() }
            } catch (e: Exception) {
                raise(
                    PersistenceError.StorageUnavailable(
                        occurredAt = Clock.System.now(),
                        operation = "findAllRoot",
                        cause = e,
                    )
                )
            }
        }
    }

    private fun rowToScope(row: io.github.kamiazya.scopes.scopemanagement.db.Scopes): Either<PersistenceError, Scope> = either {
        val scopeId = ScopeId.create(row.id).mapLeft { validationError ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "Scope",
                entityId = row.id,
                reason = "Invalid scope id in database: $validationError",
            )
        }.bind()

        // Load aspects
        val aspectRows = database.scopeAspectQueries.findByScopeId(scopeId.value).executeAsList()

        val aspectMap = buildMap<AspectKey, arrow.core.NonEmptyList<AspectValue>> {
            aspectRows.groupBy { it.aspect_key }.forEach { (keyStr, rows) ->
                val key = AspectKey.create(keyStr).mapLeft { validationError ->
                    PersistenceError.DataCorruption(
                        occurredAt = Clock.System.now(),
                        entityType = "ScopeAspect",
                        entityId = scopeId.value,
                        reason = "Invalid aspect key in database: $validationError",
                    )
                }.bind()

                val values = rows.map { aspectRow ->
                    AspectValue.create(aspectRow.aspect_value).mapLeft { validationError ->
                        PersistenceError.DataCorruption(
                            occurredAt = Clock.System.now(),
                            entityType = "ScopeAspect",
                            entityId = scopeId.value,
                            reason = "Invalid aspect value in database: $validationError",
                        )
                    }.bind()
                }

                values.toNonEmptyListOrNull()?.let {
                    put(key, it)
                } ?: raise(
                    PersistenceError.DataCorruption(
                        occurredAt = Clock.System.now(),
                        entityType = "ScopeAspect",
                        entityId = scopeId.value,
                        reason = "Aspect key exists without values in database - data integrity violation",
                    )
                )
            }
        }

        val title = ScopeTitle.create(row.title).mapLeft { validationError ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "Scope",
                entityId = row.id,
                reason = "Invalid title in database: $validationError",
            )
        }.bind()

        val description = row.description?.let { desc ->
            ScopeDescription.create(desc).mapLeft { validationError ->
                PersistenceError.DataCorruption(
                    occurredAt = Clock.System.now(),
                    entityType = "Scope",
                    entityId = row.id,
                    reason = "Invalid description in database: $validationError",
                )
            }.bind()
        }

        val parentId = row.parent_id?.let { pid ->
            ScopeId.create(pid).mapLeft { validationError ->
                PersistenceError.DataCorruption(
                    occurredAt = Clock.System.now(),
                    entityType = "Scope",
                    entityId = row.id,
                    reason = "Invalid parent id in database: $validationError",
                )
            }.bind()
        }

        Scope(
            id = scopeId,
            title = title,
            description = description,
            parentId = parentId,
            status = ScopeStatus.default(),
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
    ): Either<PersistenceError, Scope> = either {
        val scopeId = ScopeId.create(row.id).mapLeft { validationError ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "Scope",
                entityId = row.id,
                reason = "Invalid scope id in database: $validationError",
            )
        }.bind()

        val aspectMap = buildMap<AspectKey, arrow.core.NonEmptyList<AspectValue>> {
            aspectRows.groupBy { it.aspect_key }.forEach { (keyStr, rows) ->
                val key = AspectKey.create(keyStr).mapLeft { validationError ->
                    PersistenceError.DataCorruption(
                        occurredAt = Clock.System.now(),
                        entityType = "ScopeAspect",
                        entityId = scopeId.value,
                        reason = "Invalid aspect key in database: $validationError",
                    )
                }.bind()

                val values = rows.map { aspectRow ->
                    AspectValue.create(aspectRow.aspect_value).mapLeft { validationError ->
                        PersistenceError.DataCorruption(
                            occurredAt = Clock.System.now(),
                            entityType = "ScopeAspect",
                            entityId = scopeId.value,
                            reason = "Invalid aspect value in database: $validationError",
                        )
                    }.bind()
                }

                values.toNonEmptyListOrNull()?.let {
                    put(key, it)
                } ?: raise(
                    PersistenceError.DataCorruption(
                        occurredAt = Clock.System.now(),
                        entityType = "ScopeAspect",
                        entityId = scopeId.value,
                        reason = "Aspect key exists without values in database - data integrity violation",
                    )
                )
            }
        }

        val title = ScopeTitle.create(row.title).mapLeft { validationError ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "Scope",
                entityId = row.id,
                reason = "Invalid title in database: $validationError",
            )
        }.bind()

        val description = row.description?.let { desc ->
            ScopeDescription.create(desc).mapLeft { validationError ->
                PersistenceError.DataCorruption(
                    occurredAt = Clock.System.now(),
                    entityType = "Scope",
                    entityId = row.id,
                    reason = "Invalid description in database: $validationError",
                )
            }.bind()
        }

        val parentId = row.parent_id?.let { pid ->
            ScopeId.create(pid).mapLeft { validationError ->
                PersistenceError.DataCorruption(
                    occurredAt = Clock.System.now(),
                    entityType = "Scope",
                    entityId = row.id,
                    reason = "Invalid parent id in database: $validationError",
                )
            }.bind()
        }

        Scope(
            id = scopeId,
            title = title,
            description = description,
            parentId = parentId,
            status = ScopeStatus.default(),
            aspects = if (aspectMap.isEmpty()) Aspects.empty() else Aspects.from(aspectMap),
            createdAt = Instant.fromEpochMilliseconds(row.created_at),
            updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
        )
    }

    override suspend fun countByAspectKey(aspectKey: AspectKey): Either<PersistenceError, Int> = withContext(Dispatchers.IO) {
        try {
            val count = database.scopeAspectQueries.countByAspectKey(aspectKey.value).executeAsOne()
            count.toInt().right()
        } catch (e: Exception) {
            PersistenceError.StorageUnavailable(
                occurredAt = Clock.System.now(),
                operation = "countByAspectKey",
                cause = e,
            ).left()
        }
    }
}
