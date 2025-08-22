package io.github.kamiazya.scopes.scopemanagement.domain.error

import kotlinx.datetime.Instant

/**
 * Errors related to input values when creating or editing Scopes.
 */
sealed class ScopeInputError : ScopesError() {

    /**
     * Errors related to Scope IDs.
     */
    sealed class IdError : ScopeInputError() {

        data class Blank(override val occurredAt: Instant, val attemptedValue: String) : IdError()

        data class InvalidFormat(override val occurredAt: Instant, val attemptedValue: String, val expectedFormat: String = "ULID") : IdError()
    }

    /**
     * Errors related to Scope titles.
     */
    sealed class TitleError : ScopeInputError() {

        data class Empty(override val occurredAt: Instant, val attemptedValue: String) : TitleError()

        data class TooShort(override val occurredAt: Instant, val attemptedValue: String, val minimumLength: Int) : TitleError()

        data class TooLong(override val occurredAt: Instant, val attemptedValue: String, val maximumLength: Int) : TitleError()

        data class ContainsProhibitedCharacters(override val occurredAt: Instant, val attemptedValue: String, val prohibitedCharacters: List<Char>) :
            TitleError()
    }

    /**
     * Errors related to Scope descriptions.
     */
    sealed class DescriptionError : ScopeInputError() {

        data class TooLong(override val occurredAt: Instant, val attemptedValue: String, val maximumLength: Int) : DescriptionError()
    }

    /**
     * Errors related to Scope aliases.
     */
    sealed class AliasError : ScopeInputError() {

        data class Empty(override val occurredAt: Instant, val attemptedValue: String) : AliasError()

        data class TooShort(override val occurredAt: Instant, val attemptedValue: String, val minimumLength: Int) : AliasError()

        data class TooLong(override val occurredAt: Instant, val attemptedValue: String, val maximumLength: Int) : AliasError()

        data class InvalidFormat(override val occurredAt: Instant, val attemptedValue: String, val expectedPattern: String) : AliasError()
    }
}
