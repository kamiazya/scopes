package io.github.kamiazya.scopes.scopemanagement.application.error

/**
 * Errors related to scope input validation.
 */
sealed class ScopeInputError : ScopeManagementApplicationError() {
    // ID related errors
    data class IdBlank(val attemptedValue: String) : ScopeInputError()
    data class IdInvalidFormat(val attemptedValue: String, val expectedFormat: String = "ULID") : ScopeInputError()

    // Title related errors
    data class TitleEmpty(val attemptedValue: String) : ScopeInputError()
    data class TitleTooShort(val attemptedValue: String, val minimumLength: Int) : ScopeInputError()
    data class TitleTooLong(val attemptedValue: String, val maximumLength: Int) : ScopeInputError()
    data class TitleContainsProhibitedCharacters(val attemptedValue: String, val prohibitedCharacters: List<Char> = listOf('<', '>', '&', '"')) :
        ScopeInputError()

    // Description related errors
    data class DescriptionTooLong(val attemptedValue: String, val maximumLength: Int) : ScopeInputError()

    // Alias related errors
    data class AliasEmpty(val attemptedValue: String) : ScopeInputError()
    data class AliasTooShort(val attemptedValue: String, val minimumLength: Int) : ScopeInputError()
    data class AliasTooLong(val attemptedValue: String, val maximumLength: Int) : ScopeInputError()
    data class AliasInvalidFormat(val attemptedValue: String, val expectedPattern: String) : ScopeInputError()

    data class InvalidAlias(val alias: String) : ScopeInputError()

    data class AliasNotFound(val alias: String) : ScopeInputError()

    data class AliasDuplicate(val alias: String) : ScopeInputError()

    data object CannotRemoveCanonicalAlias : ScopeInputError()

    data class AliasOfDifferentScope(val alias: String, val expectedScopeId: String, val actualScopeId: String) : ScopeInputError()

    data class InvalidParentId(val parentId: String) : ScopeInputError()
}
