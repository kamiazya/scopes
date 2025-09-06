package io.github.kamiazya.scopes.scopemanagement.domain.error

import kotlinx.datetime.Instant

/**
 * Errors related to AggregateId creation and parsing.
 */
sealed class AggregateIdError : ScopesError() {

    data class InvalidType(override val occurredAt: Instant, val attemptedType: String, val validTypes: Set<String>) : AggregateIdError()

    data class InvalidIdFormat(override val occurredAt: Instant, val attemptedId: String, val expectedFormat: String) : AggregateIdError()

    data class InvalidUriFormat(override val occurredAt: Instant, val attemptedUri: String, val reason: String) : AggregateIdError()

    data class EmptyValue(override val occurredAt: Instant, val field: String) : AggregateIdError()

    data class InvalidFormat(override val occurredAt: Instant, val value: String, val formatError: FormatError) : AggregateIdError()

    enum class FormatError {
        INVALID_SCHEME,
        MISSING_TYPE,
        MISSING_ID,
        MALFORMED_URI,
        UNSUPPORTED_CHARACTERS,
    }
}
