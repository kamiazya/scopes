package io.github.kamiazya.scopes.domain.error

import kotlinx.datetime.Instant

/**
 * Errors specific to EventId operations.
 * 
 * These errors represent violations of event ID constraints:
 * - Format violations
 * - Invalid event types
 * - Parsing errors
 */
sealed class EventIdError : ConceptualModelError() {
    
    /**
     * The value provided for EventId is empty.
     */
    data class EmptyValue(
        override val occurredAt: Instant,
        val field: String
    ) : EventIdError()
    
    /**
     * Invalid event type provided.
     */
    data class InvalidEventType(
        override val occurredAt: Instant,
        val attemptedType: String,
        val reason: String
    ) : EventIdError()
    
    /**
     * Invalid URI format for EventId.
     */
    data class InvalidUriFormat(
        override val occurredAt: Instant,
        val attemptedUri: String,
        val reason: String
    ) : EventIdError()
    
    /**
     * ULID generation or parsing failed.
     */
    data class UlidError(
        override val occurredAt: Instant,
        val reason: String
    ) : EventIdError()
}