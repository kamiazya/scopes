package io.github.kamiazya.scopes.collaborativeversioning.application.error

import io.github.kamiazya.scopes.platform.domain.value.EventId

/**
 * Errors that can occur when handling domain events.
 */
sealed class EventHandlingError(recoverable: Boolean = true) : ApplicationError(recoverable) {
    /**
     * Handler is not configured to process this event type.
     */
    data class UnsupportedEventType(val eventType: String, val handlerName: String) : EventHandlingError()

    /**
     * Failed to process the event due to business logic error.
     */
    data class ProcessingFailed(val eventId: EventId, val eventType: String, val reason: String) : EventHandlingError()

    /**
     * Event data is invalid or corrupted.
     */
    data class InvalidEventData(val eventId: EventId, val eventType: String, val details: String) : EventHandlingError()

    /**
     * Required resource not found when handling event.
     */
    data class ResourceNotFound(val eventId: EventId, val resourceType: String, val resourceId: String) : EventHandlingError()

    /**
     * Multiple handlers failed to process the event.
     */
    data class MultipleHandlersFailed(val errors: List<EventHandlingError>) : EventHandlingError(recoverable = false)

    /**
     * Timeout occurred while handling the event.
     */
    data class HandlingTimeout(val eventId: EventId, val eventType: String, val timeoutMs: Long) : EventHandlingError()

    /**
     * Infrastructure error during event handling.
     */
    data class InfrastructureError(val message: String) : EventHandlingError(recoverable = false)

    /**
     * Unexpected error during event handling.
     */
    data class UnexpectedError(val message: String) : EventHandlingError(recoverable = false)
}
