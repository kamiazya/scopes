package io.github.kamiazya.scopes.scopemanagement.domain.error

import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Error when a scope is not found.
 */
data class ScopeNotFoundError(val scopeId: ScopeId, override val occurredAt: Instant = Clock.System.now()) : ScopesError()
