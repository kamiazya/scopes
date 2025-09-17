package io.github.kamiazya.scopes.scopemanagement.application.error

import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError

/**
 * Presenter for converting domain scope input errors to human-readable messages.
 * This service handles the presentation concern of error messages.
 */
class ScopeInputErrorPresenter {

    fun presentIdFormat(formatType: ScopeInputError.IdError.InvalidIdFormat.IdFormatType): String = when (formatType) {
        ScopeInputError.IdError.InvalidIdFormat.IdFormatType.ULID -> "ULID format"
        ScopeInputError.IdError.InvalidIdFormat.IdFormatType.UUID -> "UUID format"
        ScopeInputError.IdError.InvalidIdFormat.IdFormatType.NUMERIC_ID -> "numeric ID format"
        ScopeInputError.IdError.InvalidIdFormat.IdFormatType.CUSTOM_FORMAT -> "custom format"
    }

    fun presentAliasPattern(patternType: ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType): String = when (patternType) {
        ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.LOWERCASE_WITH_HYPHENS -> "lowercase letters with hyphens"
        ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.ALPHANUMERIC -> "alphanumeric characters"
        ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.ULID_LIKE -> "ULID-like format"
        ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.CUSTOM_PATTERN -> "custom pattern"
    }
}
