package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Scope-related domain errors.
 */
sealed class ScopeError : DomainError() {
    object ScopeNotFound : ScopeError()
    data class InvalidTitle(val reason: String) : ScopeError()
    data class InvalidDescription(val reason: String) : ScopeError()
    data class InvalidParent(val parentId: ScopeId, val reason: String) : ScopeError()
    data class CircularReference(val scopeId: ScopeId, val parentId: ScopeId) : ScopeError()
    object SelfParenting : ScopeError()
}