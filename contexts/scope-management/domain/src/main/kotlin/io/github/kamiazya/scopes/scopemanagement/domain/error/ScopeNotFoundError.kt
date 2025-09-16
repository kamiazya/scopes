package io.github.kamiazya.scopes.scopemanagement.domain.error

import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Error when a scope is not found.
 */
data class ScopeNotFoundError(val scopeId: ScopeId) : ScopesError()
