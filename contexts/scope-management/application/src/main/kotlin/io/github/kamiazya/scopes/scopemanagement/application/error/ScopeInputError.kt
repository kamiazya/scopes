package io.github.kamiazya.scopes.scopemanagement.application.error

/**
 * Errors related to scope input validation.
 */
sealed class ScopeInputError(recoverable: Boolean = true) : ApplicationError(recoverable) {
    data class IdBlank(val attemptedValue: String) : ScopeInputError()
    data class IdInvalidFormat(val attemptedValue: String, val expectedFormat: String = "ULID") : ScopeInputError()

    data class TitleEmpty(val attemptedValue: String) : ScopeInputError()
    data class TitleTooShort(val attemptedValue: String, val minimumLength: Int) : ScopeInputError()
    data class TitleTooLong(val attemptedValue: String, val maximumLength: Int) : ScopeInputError()
    data class TitleContainsProhibitedCharacters(val attemptedValue: String, val prohibitedCharacters: List<Char>) : ScopeInputError()

    data class DescriptionTooLong(val attemptedValue: String, val maximumLength: Int) : ScopeInputError()

    data class AliasEmpty(val attemptedValue: String) : ScopeInputError()
    data class AliasTooShort(val attemptedValue: String, val minimumLength: Int) : ScopeInputError()
    data class AliasTooLong(val attemptedValue: String, val maximumLength: Int) : ScopeInputError()
    data class AliasInvalidFormat(val attemptedValue: String, val expectedPattern: String) : ScopeInputError()

    data class InvalidAlias(val attemptedValue: String) : ScopeInputError()

    data class AliasNotFound(val attemptedValue: String) : ScopeInputError()

    data class AliasDuplicate(val attemptedValue: String) : ScopeInputError()

    data class CannotRemoveCanonicalAlias(val attemptedValue: String) : ScopeInputError()

    data class AliasOfDifferentScope(val attemptedValue: String) : ScopeInputError()

    data class InvalidParentId(val attemptedValue: String) : ScopeInputError()
}
