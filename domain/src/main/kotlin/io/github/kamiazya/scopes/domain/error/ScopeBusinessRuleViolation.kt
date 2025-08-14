package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Scope business rule violations.
 */
sealed class ScopeBusinessRuleViolation : DomainError() {
    data class ScopeMaxDepthExceeded(
        val maxDepth: Int,
        val actualDepth: Int
    ) : ScopeBusinessRuleViolation()
    data class ScopeMaxChildrenExceeded(
        val maxChildren: Int,
        val actualChildren: Int
    ) : ScopeBusinessRuleViolation()
    data class ScopeDuplicateTitle(val title: String, val parentId: ScopeId?) : ScopeBusinessRuleViolation()
}
