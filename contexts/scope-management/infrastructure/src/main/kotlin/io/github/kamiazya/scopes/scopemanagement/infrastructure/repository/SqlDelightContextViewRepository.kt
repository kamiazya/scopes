package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.db.Context_views
import io.github.kamiazya.scopes.scopemanagement.db.ScopeManagementDatabase
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewName
import kotlinx.datetime.Instant

/**
 * SQLDelight implementation of ContextViewRepository.
 */
class SqlDelightContextViewRepository(private val database: ScopeManagementDatabase) : ContextViewRepository {

    override suspend fun save(contextView: ContextView): Either<Any, ContextView> = try {
        val existing = database.contextViewQueries.findById(contextView.id.value).executeAsOneOrNull()

        if (existing != null) {
            database.contextViewQueries.updateContextView(
                key = contextView.key.value,
                name = contextView.name.value,
                description = contextView.description?.value,
                filter = contextView.filter.expression,
                updated_at = contextView.updatedAt.toEpochMilliseconds(),
                id = contextView.id.value,
            )
        } else {
            database.contextViewQueries.insertContextView(
                id = contextView.id.value,
                key = contextView.key.value,
                name = contextView.name.value,
                description = contextView.description?.value,
                filter = contextView.filter.expression,
                created_at = contextView.createdAt.toEpochMilliseconds(),
                updated_at = contextView.updatedAt.toEpochMilliseconds(),
            )
        }
        contextView.right()
    } catch (e: Exception) {
        e.left()
    }

    override suspend fun findById(id: ContextViewId): Either<Any, ContextView?> = try {
        val result = database.contextViewQueries.findById(id.value).executeAsOneOrNull()
        result?.let { rowToContextView(it) }.right()
    } catch (e: Exception) {
        e.left()
    }

    override suspend fun findByKey(key: ContextViewKey): Either<Any, ContextView?> = try {
        val result = database.contextViewQueries.findByKey(key.value).executeAsOneOrNull()
        result?.let { rowToContextView(it) }.right()
    } catch (e: Exception) {
        e.left()
    }

    override suspend fun findByName(name: ContextViewName): Either<Any, ContextView?> = try {
        val result = database.contextViewQueries.findByName(name.value).executeAsOneOrNull()
        result?.let { rowToContextView(it) }.right()
    } catch (e: Exception) {
        e.left()
    }

    override suspend fun findAll(): Either<Any, List<ContextView>> = try {
        val results = database.contextViewQueries.getAllViews().executeAsList()
        results.map { rowToContextView(it) }.right()
    } catch (e: Exception) {
        e.left()
    }

    override suspend fun deleteById(id: ContextViewId): Either<Any, Boolean> = try {
        database.contextViewQueries.deleteById(id.value)
        true.right()
    } catch (e: Exception) {
        e.left()
    }

    override suspend fun existsByKey(key: ContextViewKey): Either<Any, Boolean> = try {
        val result = database.contextViewQueries.existsByKey(key.value).executeAsOne()
        result.right()
    } catch (e: Exception) {
        e.left()
    }

    override suspend fun existsByName(name: ContextViewName): Either<Any, Boolean> = try {
        val result = database.contextViewQueries.existsByName(name.value).executeAsOne()
        result.right()
    } catch (e: Exception) {
        e.left()
    }

    private fun rowToContextView(row: Context_views): ContextView {
        val id = ContextViewId.create(row.id).fold(
            ifLeft = { error("Invalid id in database: $it") },
            ifRight = { it },
        )
        val key = ContextViewKey.create(row.key).fold(
            ifLeft = { error("Invalid key in database: $it") },
            ifRight = { it },
        )
        val name = ContextViewName.create(row.name).fold(
            ifLeft = { error("Invalid name in database: $it") },
            ifRight = { it },
        )
        val description = row.description?.let { desc ->
            ContextViewDescription.create(desc).fold(
                ifLeft = { error("Invalid description in database: $it") },
                ifRight = { it },
            )
        }
        val filter = ContextViewFilter.create(row.filter).fold(
            ifLeft = { error("Invalid filter in database: $it") },
            ifRight = { it },
        )

        return ContextView(
            _id = id,
            key = key,
            name = name,
            description = description,
            filter = filter,
            createdAt = Instant.fromEpochMilliseconds(row.created_at),
            updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
        )
    }
}
