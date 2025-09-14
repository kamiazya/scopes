package io.github.kamiazya.scopes.scopemanagement.domain.error

/**
 * Errors related to AggregateId creation and parsing.
 */
sealed class AggregateIdError : ScopesError() {

    data class InvalidFormat(val value: String, val formatError: FormatError) : AggregateIdError()

    enum class FormatError {
        INVALID_SCHEME,
        MISSING_TYPE,
        MISSING_ID,
        MALFORMED_URI,
        UNSUPPORTED_CHARACTERS,
    }
}
