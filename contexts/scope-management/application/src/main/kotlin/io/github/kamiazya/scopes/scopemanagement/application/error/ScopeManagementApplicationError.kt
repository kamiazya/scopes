package io.github.kamiazya.scopes.scopemanagement.application.error

import io.github.kamiazya.scopes.platform.application.error.ApplicationError

/**
 * Type-safe structured error information for scope management bounded context.
 * This sealed class hierarchy provides compile-time safety for error handling,
 * containing only structured data without presentation-specific messages.
 * The presentation layer is responsible for generating user-facing messages.
 */
sealed class ScopeManagementApplicationError : ApplicationError {

    /**
     * Errors related to data persistence operations.
     */
    sealed class PersistenceError : ScopeManagementApplicationError() {
        data class StorageUnavailable(val operation: String) : PersistenceError()

        data class DataCorruption(val entityType: String, val entityId: String?, val reason: String) : PersistenceError()

        data class ConcurrencyConflict(val entityType: String, val entityId: String, val expectedVersion: String, val actualVersion: String) :
            PersistenceError()

        data class NotFound(val entityType: String, val entityId: String?) : PersistenceError()

        data class ProjectionFailed(val eventType: String, val aggregateId: String, val reason: String) : PersistenceError()
    }
}
