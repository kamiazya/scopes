package io.github.kamiazya.scopes.scopemanagement.domain.error

/**
 * Errors related to data persistence.
 */
sealed class PersistenceError : ScopesError() {

    data class ConcurrencyConflict(val entityType: String, val entityId: String, val expectedVersion: String, val actualVersion: String) : PersistenceError()
}
