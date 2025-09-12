package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.scopemanagement.db.Context_views
import io.github.kamiazya.scopes.scopemanagement.db.GetActiveContext
import io.github.kamiazya.scopes.scopemanagement.db.ScopeManagementDatabase
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ActiveContextRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * SQLite implementation of ActiveContextRepository.
 * Manages the persistence of the currently active context view.
 */
class ActiveContextRepositoryImpl(private val database: ScopeManagementDatabase) : ActiveContextRepository {

    @Volatile
    private var initialized = false
    private val initMutex = Mutex()

    /**
     * Initialize the active context table on first use.
     */
    suspend fun initialize(): Either<ScopesError, Unit> = withContext(Dispatchers.IO) {
        either {
            try {
                database.activeContextQueries.initializeActiveContext(
                    updated_at = Clock.System.now().toEpochMilliseconds(),
                )
            } catch (e: Exception) {
                raise(
                    PersistenceError.StorageUnavailable(
                        operation = "initializeActiveContext",
                        cause = e,
                    ),
                )
            }
        }
    }

    /**
     * Ensures the active context table is initialized before any operation.
     */
    private suspend fun ensureInitialized(): Either<ScopesError, Unit> = withContext(Dispatchers.IO) {
        if (initialized) {
            Either.Right(Unit)
        } else {
            initMutex.withLock {
                if (initialized) {
                    Either.Right(Unit)
                } else {
                    initialize().onRight {
                        initialized = true
                    }
                }
            }
        }
    }

    override suspend fun getActiveContext(): Either<ScopesError, ContextView?> = withContext(Dispatchers.IO) {
        either {
            ensureInitialized().bind()
            try {
                val result = database.activeContextQueries.getActiveContext().executeAsOneOrNull()
                result?.let { activeContextToContextView(it) }
            } catch (e: Exception) {
                raise(
                    PersistenceError.StorageUnavailable(
                        operation = "getActiveContext",
                        cause = e,
                    ),
                )
            }
        }
    }

    override suspend fun setActiveContext(contextView: ContextView): Either<ScopesError, Unit> = withContext(Dispatchers.IO) {
        either {
            ensureInitialized().bind()
            try {
                database.activeContextQueries.setActiveContext(
                    context_view_id = contextView.id.value,
                    updated_at = Clock.System.now().toEpochMilliseconds(),
                )
            } catch (e: Exception) {
                raise(
                    PersistenceError.StorageUnavailable(
                        operation = "setActiveContext",
                        cause = e,
                    ),
                )
            }
        }
    }

    override suspend fun clearActiveContext(): Either<ScopesError, Unit> = withContext(Dispatchers.IO) {
        either {
            ensureInitialized().bind()
            try {
                database.activeContextQueries.clearActiveContext(
                    updated_at = Clock.System.now().toEpochMilliseconds(),
                )
            } catch (e: Exception) {
                raise(
                    PersistenceError.StorageUnavailable(
                        operation = "clearActiveContext",
                        cause = e,
                    ),
                )
            }
        }
    }

    override suspend fun hasActiveContext(): Either<ScopesError, Boolean> = withContext(Dispatchers.IO) {
        either {
            ensureInitialized().bind()
            try {
                database.activeContextQueries.hasActiveContext().executeAsOne()
            } catch (e: Exception) {
                raise(
                    PersistenceError.StorageUnavailable(
                        operation = "hasActiveContext",
                        cause = e,
                    ),
                )
            }
        }
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
            id = id,
            key = key,
            name = name,
            description = description,
            filter = filter,
            createdAt = Instant.fromEpochMilliseconds(row.created_at),
            updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
        )
    }

    private fun activeContextToContextView(row: GetActiveContext): ContextView? {
        // All fields except id can be null due to LEFT JOIN
        if (row.key == null ||
            row.name == null ||
            row.filter == null ||
            row.created_at == null ||
            row.updated_at == null
        ) {
            return null
        }

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
            id = id,
            key = key,
            name = name,
            description = description,
            filter = filter,
            createdAt = Instant.fromEpochMilliseconds(row.created_at),
            updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
        )
    }
}
