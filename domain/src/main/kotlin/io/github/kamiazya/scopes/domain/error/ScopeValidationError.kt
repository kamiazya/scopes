package io.github.kamiazya.scopes.domain.error

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