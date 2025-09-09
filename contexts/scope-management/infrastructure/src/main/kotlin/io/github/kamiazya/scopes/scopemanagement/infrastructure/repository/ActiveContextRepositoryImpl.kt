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
                        occurredAt = Clock.System.now(),
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
                result?.let { activeContextToContextView(it).bind() } ?: null
            } catch (e: Exception) {
                raise(
                    PersistenceError.StorageUnavailable(
                        occurredAt = Clock.System.now(),
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
                        occurredAt = Clock.System.now(),
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
                        occurredAt = Clock.System.now(),
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
                        occurredAt = Clock.System.now(),
                        operation = "hasActiveContext",
                        cause = e,
                    ),
                )
            }
        }
    }

    private fun rowToContextView(row: Context_views): Either<PersistenceError, ContextView> = either {
        val id = ContextViewId.create(row.id).mapLeft { validationError ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "ContextView",
                entityId = row.id,
                reason = "Invalid id in database: $validationError",
            )
        }.bind()
        
        val key = ContextViewKey.create(row.key).mapLeft { validationError ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "ContextView",
                entityId = row.id,
                reason = "Invalid key in database: $validationError",
            )
        }.bind()
        
        val name = ContextViewName.create(row.name).mapLeft { validationError ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "ContextView",
                entityId = row.id,
                reason = "Invalid name in database: $validationError",
            )
        }.bind()
        
        val description = row.description?.let { desc ->
            ContextViewDescription.create(desc).mapLeft { validationError ->
                PersistenceError.DataCorruption(
                    occurredAt = Clock.System.now(),
                    entityType = "ContextView",
                    entityId = row.id,
                    reason = "Invalid description in database: $validationError",
                )
            }.bind()
        }
        
        val filter = ContextViewFilter.create(row.filter).mapLeft { validationError ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "ContextView",
                entityId = row.id,
                reason = "Invalid filter in database: $validationError",
            )
        }.bind()

        ContextView(
            id = id,
            key = key,
            name = name,
            description = description,
            filter = filter,
            createdAt = Instant.fromEpochMilliseconds(row.created_at),
            updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
        )
    }

    private fun activeContextToContextView(row: GetActiveContext): Either<PersistenceError, ContextView?> = either {
        // All fields except id can be null due to LEFT JOIN
        if (row.key == null ||
            row.name == null ||
            row.filter == null ||
            row.created_at == null ||
            row.updated_at == null
        ) {
            return@either null
        }

        val id = ContextViewId.create(row.id).mapLeft { validationError ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "ContextView",
                entityId = row.id,
                reason = "Invalid id in database: $validationError",
            )
        }.bind()
        
        val key = ContextViewKey.create(row.key).mapLeft { validationError ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "ContextView",
                entityId = row.id,
                reason = "Invalid key in database: $validationError",
            )
        }.bind()
        
        val name = ContextViewName.create(row.name).mapLeft { validationError ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "ContextView",
                entityId = row.id,
                reason = "Invalid name in database: $validationError",
            )
        }.bind()
        
        val description = row.description?.let { desc ->
            ContextViewDescription.create(desc).mapLeft { validationError ->
                PersistenceError.DataCorruption(
                    occurredAt = Clock.System.now(),
                    entityType = "ContextView",
                    entityId = row.id,
                    reason = "Invalid description in database: $validationError",
                )
            }.bind()
        }
        
        val filter = ContextViewFilter.create(row.filter).mapLeft { validationError ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "ContextView",
                entityId = row.id,
                reason = "Invalid filter in database: $validationError",
            )
        }.bind()

        ContextView(
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
