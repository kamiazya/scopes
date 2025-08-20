package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import kotlinx.datetime.Instant

/**
 * Errors specific to Scope operations.
 */
sealed class ScopeOperationError : UserIntentionError() {

    data class OperationOnArchivedScope(override val occurredAt: Instant, val scopeId: ScopeId, val operation: String) : ScopeOperationError()

    data class AlreadyArchived(override val occurredAt: Instant, val scopeId: ScopeId) : ScopeOperationError()

    data class NotArchived(override val occurredAt: Instant, val scopeId: ScopeId) : ScopeOperationError()

    data class AspectNotFound(override val occurredAt: Instant, val scopeId: ScopeId, val aspectKey: AspectKey) : ScopeOperationError()

    data class AspectValueNotFound(
        override val occurredAt: Instant,
        val scopeId: ScopeId,
        val aspectKey: AspectKey,
        val aspectValue: AspectValue,
    ) : ScopeOperationError()
}
