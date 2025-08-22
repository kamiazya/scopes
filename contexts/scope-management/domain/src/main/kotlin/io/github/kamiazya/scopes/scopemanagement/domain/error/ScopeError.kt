package io.github.kamiazya.scopes.scopemanagement.domain.error

import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Errors related to Scope operations.
 */
sealed class ScopeError : ScopesError() {

    /**
     * Scope not found error.
     */
    data class NotFound(val scopeId: ScopeId, override val occurredAt: Instant = Clock.System.now()) : ScopeError()

    /**
     * Parent scope not found error.
     */
    data class ParentNotFound(val parentId: ScopeId, override val occurredAt: Instant = Clock.System.now()) : ScopeError()

    /**
     * Duplicate title error.
     */
    data class DuplicateTitle(val title: String, val parentId: ScopeId?, override val occurredAt: Instant = Clock.System.now()) : ScopeError()

    /**
     * Scope already deleted error.
     */
    data class AlreadyDeleted(val scopeId: ScopeId, override val occurredAt: Instant = Clock.System.now()) : ScopeError()

    /**
     * Scope already archived error.
     */
    data class AlreadyArchived(val scopeId: ScopeId, override val occurredAt: Instant = Clock.System.now()) : ScopeError()

    /**
     * Scope not archived error.
     */
    data class NotArchived(val scopeId: ScopeId, override val occurredAt: Instant = Clock.System.now()) : ScopeError()

    /**
     * Version mismatch error for optimistic concurrency control.
     */
    data class VersionMismatch(
        val scopeId: ScopeId,
        val expectedVersion: Long,
        val actualVersion: Long,
        override val occurredAt: Instant = Clock.System.now(),
    ) : ScopeError()
}
