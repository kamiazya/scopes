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

    /**
     * Invalid event type provided.
     */

    /**
     * Invalid URI format for EventId.
     */

    /**
     * ULID generation or parsing failed.
     */
}
