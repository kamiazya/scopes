package io.github.kamiazya.scopes.scopemanagement.domain.error

import kotlinx.datetime.Instant

/**
 * Errors related to data persistence.
 */
sealed class PersistenceError : ScopesError() {

    data class StorageUnavailable(override val occurredAt: Instant, val operation: String, val cause: Throwable?) : PersistenceError()

    data class DataCorruption(override val occurredAt: Instant, val entityType: String, val entityId: String?, val reason: String) : PersistenceError()

    data class ConcurrencyConflict(
        override val occurredAt: Instant,
        val entityType: String,
        val entityId: String,
        val expectedVersion: String,
        val actualVersion: String,
    ) : PersistenceError()
}
