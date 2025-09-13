package io.github.kamiazya.scopes.scopemanagement.domain.error

/**
 * Errors related to AggregateId creation and parsing.
 */
sealed class AggregateIdError : ScopesError() {

    data class InvalidType(val attemptedType: String, val validTypes: Set<String>) : AggregateIdError()

    data class InvalidIdFormat(val attemptedId: String, val expectedFormat: String) : AggregateIdError()

    data class InvalidUriFormat(val attemptedUri: String, val reason: String) : AggregateIdError()

    data class EmptyValue(val field: String) : AggregateIdError()

    data class InvalidFormat(val value: String, val formatError: FormatError) : AggregateIdError()

    enum class FormatError {
        INVALID_SCHEME,
        MISSING_TYPE,
        MISSING_ID,
        MALFORMED_URI,
        UNSUPPORTED_CHARACTERS,
    }
}
