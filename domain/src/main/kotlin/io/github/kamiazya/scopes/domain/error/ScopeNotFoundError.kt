package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import kotlinx.datetime.Instant

/**
 * Error for when a scope is not found.
 */
data class ScopeNotFoundError(
    override val occurredAt: Instant,
    val scopeId: ScopeId
) : UserIntentionError()
