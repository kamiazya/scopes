package io.github.kamiazya.scopes.scopemanagement.application.error

/**
 * Errors related to data persistence operations.
 */
sealed class PersistenceError(recoverable: Boolean = false) : ApplicationError(recoverable) {
    data class StorageUnavailable(val operation: String, val cause: String?) : PersistenceError()

    data class DataCorruption(val entityType: String, val entityId: String?, val reason: String) : PersistenceError()

    data class ConcurrencyConflict(val entityType: String, val entityId: String, val expectedVersion: String, val actualVersion: String) :
        PersistenceError()
}
