package io.github.kamiazya.scopes.scopemanagement.application.error

/**
 * Errors related to scope input validation.
 * All values are sanitized previews to prevent leaking sensitive user input.
 */
sealed class ScopeInputError : ScopeManagementApplicationError() {
    // ID related errors
    data class IdBlank(val preview: String) : ScopeInputError()
    data class IdInvalidFormat(val preview: String, val expectedFormat: String = "ULID") : ScopeInputError()

    // Title related errors
    data class TitleEmpty(val preview: String) : ScopeInputError()
    data class TitleTooShort(val preview: String, val minimumLength: Int) : ScopeInputError()
    data class TitleTooLong(val preview: String, val maximumLength: Int) : ScopeInputError()
    data class TitleContainsProhibitedCharacters(val preview: String, val prohibitedCharacters: List<Char> = listOf('<', '>', '&', '"')) :
        ScopeInputError()

    // Description related errors
    data class DescriptionTooLong(val preview: String, val maximumLength: Int) : ScopeInputError()

    // Alias related errors
    data class AliasEmpty(val preview: String) : ScopeInputError()
    data class AliasTooShort(val preview: String, val minimumLength: Int) : ScopeInputError()
    data class AliasTooLong(val preview: String, val maximumLength: Int) : ScopeInputError()
    data class AliasInvalidFormat(val preview: String, val expectedPattern: String) : ScopeInputError()

    data class InvalidAlias(val preview: String) : ScopeInputError()

    data class AliasNotFound(val preview: String) : ScopeInputError()

    data class AliasDuplicate(val preview: String) : ScopeInputError()

    data object CannotRemoveCanonicalAlias : ScopeInputError()

    data class AliasOfDifferentScope(val preview: String, val expectedScopeId: String, val actualScopeId: String) : ScopeInputError()

    data class InvalidParentId(val preview: String) : ScopeInputError()

    data class ValidationFailed(val field: String, val preview: String, val reason: String) : ScopeInputError()
}
