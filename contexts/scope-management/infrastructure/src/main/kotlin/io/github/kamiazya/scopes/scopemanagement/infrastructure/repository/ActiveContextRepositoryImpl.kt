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
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * SQLite implementation of ActiveContextRepository.
 * Manages the persistence of the currently active context view.
 */
class ActiveContextRepositoryImpl(private val database: ScopeManagementDatabase) : ActiveContextRepository {

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

    override suspend fun getActiveContext(): Either<ScopesError, ContextView?> = withContext(Dispatchers.IO) {
        either {
            try {
                val result = database.activeContextQueries.getActiveContext().executeAsOneOrNull()
                if (result == null) {
                    null
                } else {
                    activeContextToContextView(result).bind()
                }
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

    private suspend fun rowToContextView(row: Context_views): Either<ScopesError, ContextView> = either {
        val id = ContextViewId.create(row.id).mapLeft { error ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "ContextView",
                entityId = row.id,
                reason = "Invalid id in database: $error",
            )
        }.bind()

        val key = ContextViewKey.create(row.key).mapLeft { error ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "ContextView",
                entityId = row.id,
                reason = "Invalid key in database: $error",
            )
        }.bind()

        val name = ContextViewName.create(row.name).mapLeft { error ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "ContextView",
                entityId = row.id,
                reason = "Invalid name in database: $error",
            )
        }.bind()

        val description = row.description?.let { desc ->
            ContextViewDescription.create(desc).mapLeft { error ->
                PersistenceError.DataCorruption(
                    occurredAt = Clock.System.now(),
                    entityType = "ContextView",
                    entityId = row.id,
                    reason = "Invalid description in database: $error",
                )
            }.bind()
        }

        val filter = ContextViewFilter.create(row.filter).mapLeft { error ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "ContextView",
                entityId = row.id,
                reason = "Invalid filter in database: $error",
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

    private suspend fun activeContextToContextView(row: GetActiveContext): Either<ScopesError, ContextView?> = either {
        // All fields except id can be null due to LEFT JOIN
        if (row.key == null ||
            row.name == null ||
            row.filter == null ||
            row.created_at == null ||
            row.updated_at == null
        ) {
            return@either null
        }

        val id = ContextViewId.create(row.id).mapLeft { error ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "ContextView",
                entityId = row.id,
                reason = "Invalid id in database: $error",
            )
        }.bind()

        val key = ContextViewKey.create(row.key!!).mapLeft { error ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "ContextView",
                entityId = row.id,
                reason = "Invalid key in database: $error",
            )
        }.bind()

        val name = ContextViewName.create(row.name!!).mapLeft { error ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "ContextView",
                entityId = row.id,
                reason = "Invalid name in database: $error",
            )
        }.bind()

        val description = row.description?.let { desc ->
            ContextViewDescription.create(desc).mapLeft { error ->
                PersistenceError.DataCorruption(
                    occurredAt = Clock.System.now(),
                    entityType = "ContextView",
                    entityId = row.id,
                    reason = "Invalid description in database: $error",
                )
            }.bind()
        }

        val filter = ContextViewFilter.create(row.filter!!).mapLeft { error ->
            PersistenceError.DataCorruption(
                occurredAt = Clock.System.now(),
                entityType = "ContextView",
                entityId = row.id,
                reason = "Invalid filter in database: $error",
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
