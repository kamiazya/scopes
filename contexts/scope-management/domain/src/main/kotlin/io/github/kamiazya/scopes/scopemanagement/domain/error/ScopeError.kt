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

    // ===== ALIAS RELATED ERRORS =====

    /**
     * Duplicate alias error - alias name already exists for another scope.
     */
    data class DuplicateAlias(val aliasName: String, val scopeId: ScopeId) : ScopeError()

    /**
     * Alias not found error - specified alias does not exist for this scope.
     */
    data class AliasNotFound(val aliasId: String, val scopeId: ScopeId) : ScopeError()

    /**
     * Cannot remove canonical alias error - canonical aliases cannot be removed, only replaced.
     */
    data class CannotRemoveCanonicalAlias(val aliasId: String, val scopeId: ScopeId) : ScopeError()

    /**
     * No canonical alias error - scope does not have a canonical alias.
     */
    data class NoCanonicalAlias(val scopeId: ScopeId) : ScopeError()

    // ===== ASPECT RELATED ERRORS =====

    /**
     * Aspect not found error - specified aspect does not exist for this scope.
     */
    data class AspectNotFound(val aspectKey: String, val scopeId: ScopeId) : ScopeError()

    /**
     * Invalid event sequence error - events must be applied in correct order.
     */
    data class InvalidEventSequence(val message: String) : ScopeError()
}
