package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import arrow.core.NonEmptyList

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
     * Scope validation-related domain errors.
     */
    sealed class ScopeValidationError : DomainError() {
        object EmptyScopeTitle : ScopeValidationError()
        object ScopeTitleTooShort : ScopeValidationError()
        data class ScopeTitleTooLong(val maxLength: Int, val actualLength: Int) : ScopeValidationError()
        object ScopeTitleContainsNewline : ScopeValidationError()
        data class ScopeDescriptionTooLong(val maxLength: Int, val actualLength: Int) : ScopeValidationError()
        data class ScopeInvalidFormat(val field: String, val expected: String) : ScopeValidationError()
    }

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

}
