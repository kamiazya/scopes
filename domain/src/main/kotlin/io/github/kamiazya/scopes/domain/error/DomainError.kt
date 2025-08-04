package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Domain-level errors following functional DDD principles.
 * All business rule violations and domain validation errors.
 */
sealed class DomainError {

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

    /**
     * Validation-related domain errors.
     */
    sealed class ValidationError : DomainError() {
        object EmptyTitle : ValidationError()
        object TitleTooShort : ValidationError()
        data class TitleTooLong(val maxLength: Int, val actualLength: Int) : ValidationError()
        data class DescriptionTooLong(val maxLength: Int, val actualLength: Int) : ValidationError()
        data class InvalidFormat(val field: String, val expected: String) : ValidationError()
    }

    /**
     * Business rule violations.
     */
    sealed class BusinessRuleViolation : DomainError() {
        data class MaxDepthExceeded(val maxDepth: Int, val actualDepth: Int) : BusinessRuleViolation()
        data class MaxChildrenExceeded(val maxChildren: Int, val actualChildren: Int) : BusinessRuleViolation()
        data class DuplicateTitle(val title: String, val parentId: ScopeId?) : BusinessRuleViolation()
    }
}
