package io.github.kamiazya.scopes.scopemanagement.application.error

import io.github.kamiazya.scopes.platform.application.error.ApplicationError

/**
 * Type-safe structured error information for scope management bounded context.
 * This sealed class hierarchy provides compile-time safety for error handling,
 * containing only structured data without presentation-specific messages.
 * The presentation layer is responsible for generating user-facing messages.
 */
sealed class ScopeManagementApplicationError(open val recoverable: Boolean = true, override val cause: Throwable? = null) : ApplicationError {

    /**
     * Errors related to data persistence operations.
     */
    sealed class PersistenceError(recoverable: Boolean = false, cause: Throwable? = null) : ScopeManagementApplicationError(recoverable, cause) {
        data class StorageUnavailable(val operation: String, val errorCause: String?) : PersistenceError()

        data class DataCorruption(val entityType: String, val entityId: String?, val reason: String) : PersistenceError()

        data class ConcurrencyConflict(val entityType: String, val entityId: String, val expectedVersion: String, val actualVersion: String) :
            PersistenceError()

        data class NotFound(val entityType: String, val entityId: String?) : PersistenceError(recoverable = true)
    }
}
