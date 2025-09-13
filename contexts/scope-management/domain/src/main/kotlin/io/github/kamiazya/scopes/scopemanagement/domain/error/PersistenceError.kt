package io.github.kamiazya.scopes.scopemanagement.domain.error

/**
 * Errors related to data persistence.
 */
sealed class PersistenceError : ScopesError() {

    data class StorageUnavailable(val operation: String, val cause: Throwable?) : PersistenceError()

    data class DataCorruption(val entityType: String, val entityId: String?, val reason: String) : PersistenceError()

    data class ConcurrencyConflict(val entityType: String, val entityId: String, val expectedVersion: String, val actualVersion: String) : PersistenceError()

    data class NotFound(val entityType: String, val entityId: String?) : PersistenceError()
}
