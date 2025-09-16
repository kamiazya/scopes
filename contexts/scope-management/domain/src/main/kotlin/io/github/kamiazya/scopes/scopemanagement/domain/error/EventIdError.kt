package io.github.kamiazya.scopes.scopemanagement.domain.error

/**
 * Errors specific to EventId operations.
 *
 * These errors represent violations of event ID constraints:
 * - Format violations
 * - Invalid event types
 * - Parsing errors
 */
sealed class EventIdError : ScopesError() {

    /**
     * The value provided for EventId is empty.
     */
    data class EmptyValue(val field: String) : EventIdError()

    /**
     * Invalid event type provided.
     */
    data class InvalidEventType(val attemptedType: String, val reason: String) : EventIdError()

    /**
     * Invalid URI format for EventId.
     */
    data class InvalidUriFormat(val attemptedUri: String, val reason: String) : EventIdError()

    /**
     * ULID generation or parsing failed.
     */
    data class UlidError(val reason: String) : EventIdError()
}
