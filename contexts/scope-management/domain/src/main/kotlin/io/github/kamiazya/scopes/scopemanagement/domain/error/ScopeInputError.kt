package io.github.kamiazya.scopes.scopemanagement.domain.error

/**
 * Errors related to input values when creating or editing Scopes.
 */
sealed class ScopeInputError : ScopesError() {

    /**
     * Errors related to Scope IDs.
     */
    sealed class IdError : ScopeInputError() {

        data class Blank(val attemptedValue: String) : IdError()

        data class InvalidFormat(val attemptedValue: String, val formatType: IdFormatType = IdFormatType.ULID) : IdError() {
            enum class IdFormatType {
                ULID,
                UUID,
                NUMERIC_ID,
                CUSTOM_FORMAT,
            }
        }
    }

    /**
     * Errors related to Scope titles.
     */
    sealed class TitleError : ScopeInputError() {

        data class Empty(val attemptedValue: String) : TitleError()

        data class TooShort(val attemptedValue: String, val minimumLength: Int) : TitleError()

        data class TooLong(val attemptedValue: String, val maximumLength: Int) : TitleError()

        data class ContainsProhibitedCharacters(val attemptedValue: String, val prohibitedCharacters: List<Char>) : TitleError()
    }

    /**
     * Errors related to Scope descriptions.
     */
    sealed class DescriptionError : ScopeInputError() {

        data class TooLong(val attemptedValue: String, val maximumLength: Int) : DescriptionError()
    }

    /**
     * Errors related to Scope aliases.
     */
    sealed class AliasError : ScopeInputError() {

        data class Empty(val attemptedValue: String) : AliasError()

        data class TooShort(val attemptedValue: String, val minimumLength: Int) : AliasError()

        data class TooLong(val attemptedValue: String, val maximumLength: Int) : AliasError()

        data class InvalidFormat(val attemptedValue: String, val patternType: AliasPatternType) : AliasError() {
            enum class AliasPatternType {
                LOWERCASE_WITH_HYPHENS,
                ALPHANUMERIC,
                ULID_LIKE,
                CUSTOM_PATTERN,
            }
        }
    }
}
