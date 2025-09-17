package io.github.kamiazya.scopes.scopemanagement.domain.error

import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Errors related to Scope operations.
 */
sealed class ScopeError : ScopesError() {

    /**
     * Scope not found error.
     */
    data class NotFound(val scopeId: ScopeId) : ScopeError()

    /**
     * Parent scope not found error.
     */
    data class ParentNotFound(val parentId: ScopeId) : ScopeError()

    /**
     * Duplicate title error.
     */
    data class DuplicateTitle(val title: String, val parentId: ScopeId?) : ScopeError()

    /**
     * Scope already deleted error.
     */
    data class AlreadyDeleted(val scopeId: ScopeId) : ScopeError()

    /**
     * Scope already archived error.
     */
    data class AlreadyArchived(val scopeId: ScopeId) : ScopeError()

    /**
     * Scope not archived error.
     */
    data class NotArchived(val scopeId: ScopeId) : ScopeError()

    /**
     * Version mismatch error for optimistic concurrency control.
     */
    data class VersionMismatch(val scopeId: ScopeId, val expectedVersion: Long, val actualVersion: Long) : ScopeError()
}
