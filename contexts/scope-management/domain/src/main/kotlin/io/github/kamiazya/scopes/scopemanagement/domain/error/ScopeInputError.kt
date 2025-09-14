package io.github.kamiazya.scopes.scopemanagement.domain.error

/**
 * Errors related to input values when creating or editing Scopes.
 */
sealed class ScopeInputError : ScopesError() {

    /**
     * Errors related to Scope IDs.
     */
    sealed class IdError : ScopeInputError() {
        data object EmptyId : IdError()
        data class InvalidIdFormat(val id: String, val expectedFormat: IdFormatType) : IdError() {
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
        data object EmptyTitle : TitleError()
        data class TitleTooShort(val minLength: Int) : TitleError()
        data class TitleTooLong(val maxLength: Int) : TitleError()
        data class InvalidTitleFormat(val title: String) : TitleError()
    }

    /**
     * Errors related to Scope descriptions.
     */
    sealed class DescriptionError : ScopeInputError() {
        data class DescriptionTooLong(val maxLength: Int) : DescriptionError()
    }

    /**
     * Errors related to Scope aliases.
     */
    sealed class AliasError : ScopeInputError() {
        data object EmptyAlias : AliasError()
        data class AliasTooShort(val minLength: Int) : AliasError()
        data class AliasTooLong(val maxLength: Int) : AliasError()
        data class InvalidAliasFormat(val alias: String, val expectedPattern: AliasPatternType) : AliasError() {
            enum class AliasPatternType {
                LOWERCASE_WITH_HYPHENS,
                ALPHANUMERIC,
                ULID_LIKE,
                CUSTOM_PATTERN,
            }
        }
    }
}
