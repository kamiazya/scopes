package io.github.kamiazya.scopes.scopemanagement.application.error

import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError

/**
 * Presenter for converting domain scope input errors to human-readable messages.
 * This service handles the presentation concern of error messages.
 */
class ScopeInputErrorPresenter {

    fun presentIdFormat(formatType: ScopeInputError.IdError.InvalidFormat.IdFormatType): String = when (formatType) {
        ScopeInputError.IdError.InvalidFormat.IdFormatType.ULID -> "ULID format"
        ScopeInputError.IdError.InvalidFormat.IdFormatType.UUID -> "UUID format"
        ScopeInputError.IdError.InvalidFormat.IdFormatType.NUMERIC_ID -> "numeric ID format"
        ScopeInputError.IdError.InvalidFormat.IdFormatType.CUSTOM_FORMAT -> "custom format"
    }

    fun presentAliasPattern(patternType: ScopeInputError.AliasError.InvalidFormat.AliasPatternType): String = when (patternType) {
        ScopeInputError.AliasError.InvalidFormat.AliasPatternType.LOWERCASE_WITH_HYPHENS -> "lowercase letters with hyphens"
        ScopeInputError.AliasError.InvalidFormat.AliasPatternType.ALPHANUMERIC -> "alphanumeric characters"
        ScopeInputError.AliasError.InvalidFormat.AliasPatternType.ULID_LIKE -> "ULID-like format"
        ScopeInputError.AliasError.InvalidFormat.AliasPatternType.CUSTOM_PATTERN -> "custom pattern"
    }
}
