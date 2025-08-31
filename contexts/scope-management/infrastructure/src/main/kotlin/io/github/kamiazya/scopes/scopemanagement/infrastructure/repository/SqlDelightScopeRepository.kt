package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import io.github.kamiazya.scopes.scopemanagement.db.ScopeManagementDatabase
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Aspects
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * SQLDelight implementation of ScopeRepository.
 */
class SqlDelightScopeRepository(private val database: ScopeManagementDatabase) : ScopeRepository {

    override suspend fun save(scope: Scope): Either<PersistenceError, Scope> = withContext(Dispatchers.IO) {
        try {
            database.transaction {
                val existingScope = database.scopeQueries.findScopeById(scope.id.value).executeAsOneOrNull()

                if (existingScope != null) {
                    database.scopeQueries.updateScope(
                        title = scope.title.value,
                        description = scope.description?.value,
                        parent_id = scope.parentId?.value,
                        updated_at = scope.updatedAt.toEpochMilliseconds(),
                        id = scope.id.value,
                    )
                } else {
                    database.scopeQueries.insertScope(
                        id = scope.id.value,
                        title = scope.title.value,
                        description = scope.description?.value,
                        parent_id = scope.parentId?.value,
                        created_at = scope.createdAt.toEpochMilliseconds(),
                        updated_at = scope.updatedAt.toEpochMilliseconds(),
                    )
                }

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
        try {
            val scopeRow = database.scopeQueries.findScopeById(id.value).executeAsOneOrNull()
            scopeRow?.let { rowToScope(it) }.right()
        } catch (e: Exception) {
            PersistenceError.StorageUnavailable(
                occurredAt = Clock.System.now(),
                operation = "findById",
                cause = e,
            ).left()
        }
    }

    override suspend fun findAll(): Either<PersistenceError, List<Scope>> = withContext(Dispatchers.IO) {
        try {
            val scopes = database.scopeQueries.selectAll().executeAsList().map { rowToScope(it) }
            scopes.right()
        } catch (e: Exception) {
            PersistenceError.StorageUnavailable(
                occurredAt = Clock.System.now(),
                operation = "findAll",
                cause = e,
            ).left()
        }
    }

    override suspend fun findByParentId(parentId: ScopeId?): Either<PersistenceError, List<Scope>> = withContext(Dispatchers.IO) {
        try {
            val scopes = if (parentId != null) {
                database.scopeQueries.findScopesByParentId(parentId.value).executeAsList()
            } else {
                database.scopeQueries.findRootScopes().executeAsList()
            }.map { rowToScope(it) }

            scopes.right()
        } catch (e: Exception) {
            PersistenceError.StorageUnavailable(
                occurredAt = Clock.System.now(),
                operation = "findByParentId",
                cause = e,
            ).left()
        }
    }

    override suspend fun findByParentId(parentId: ScopeId?, offset: Int, limit: Int): Either<PersistenceError, List<Scope>> = withContext(Dispatchers.IO) {
        try {
            val rows = if (parentId != null) {
                database.scopeQueries.findScopesByParentIdPaged(parentId.value, limit.toLong(), offset.toLong()).executeAsList()
            } else {
                database.scopeQueries.findRootScopesPaged(limit.toLong(), offset.toLong()).executeAsList()
            }

            rows.map { rowToScope(it) }.right()
        } catch (e: Exception) {
            PersistenceError.StorageUnavailable(
                occurredAt = Clock.System.now(),
                operation = "findByParentId(offset,limit)",
                cause = e,
            ).left()
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

    override suspend fun findDescendantsOf(scopeId: ScopeId): Either<PersistenceError, List<Scope>> = withContext(Dispatchers.IO) {
        try {
            val descendants = mutableListOf<Scope>()
            val queue = mutableListOf(scopeId)

            while (queue.isNotEmpty()) {
                val currentId = queue.removeAt(0)
                val children = database.scopeQueries.findScopesByParentId(currentId.value)
                    .executeAsList()
                    .map { row ->
                        val scope = rowToScope(row)
                        queue.add(scope.id)
                        scope
                    }
                descendants.addAll(children)
            }

            descendants.right()
        } catch (e: Exception) {
            PersistenceError.StorageUnavailable(
                occurredAt = Clock.System.now(),
                operation = "findDescendantsOf",
                cause = e,
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
            id = scopeId,
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
}
