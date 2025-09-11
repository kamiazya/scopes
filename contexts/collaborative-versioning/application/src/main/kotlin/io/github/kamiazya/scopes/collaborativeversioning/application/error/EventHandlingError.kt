package io.github.kamiazya.scopes.collaborativeversioning.application.error

import io.github.kamiazya.scopes.collaborativeversioning.domain.service.SystemTimeProvider
import io.github.kamiazya.scopes.platform.application.error.ApplicationError
import io.github.kamiazya.scopes.platform.domain.value.EventId
import kotlinx.datetime.Instant

/**
 * Errors that can occur when handling domain events.
 */
sealed class EventHandlingError : ApplicationError {
    abstract override val occurredAt: Instant
    abstract override val cause: Throwable?

    /**
     * Handler is not configured to process this event type.
     */
    data class UnsupportedEventType(
        val eventType: String,
        val handlerName: String,
        override val occurredAt: Instant = SystemTimeProvider().now(),
        override val cause: Throwable? = null,
    ) : EventHandlingError()

    /**
     * Failed to process the event due to business logic error.
     */
    data class ProcessingFailed(
        val eventId: EventId,
        val eventType: String,
        val reason: String,
        override val occurredAt: Instant = SystemTimeProvider().now(),
        override val cause: Throwable? = null,
    ) : EventHandlingError()

    /**
     * Event data is invalid or corrupted.
     */
    data class InvalidEventData(
        val eventId: EventId,
        val eventType: String,
        val details: String,
        override val occurredAt: Instant = SystemTimeProvider().now(),
        override val cause: Throwable? = null,
    ) : EventHandlingError()

    /**
     * Required resource not found when handling event.
     */
    data class ResourceNotFound(
        val eventId: EventId,
        val resourceType: String,
        val resourceId: String,
        override val occurredAt: Instant = SystemTimeProvider().now(),
        override val cause: Throwable? = null,
    ) : EventHandlingError()

    /**
     * Multiple handlers failed to process the event.
     */
    data class MultipleHandlersFailed(
        val errors: List<EventHandlingError>,
        override val occurredAt: Instant = SystemTimeProvider().now(),
        override val cause: Throwable? = null,
    ) : EventHandlingError()

    /**
     * Timeout occurred while handling the event.
     */
    data class HandlingTimeout(
        val eventId: EventId,
        val eventType: String,
        val timeoutMs: Long,
        override val occurredAt: Instant = SystemTimeProvider().now(),
        override val cause: Throwable? = null,
    ) : EventHandlingError()

    /**
     * Infrastructure error during event handling.
     */
    data class InfrastructureError(val message: String, override val occurredAt: Instant = SystemTimeProvider().now(), override val cause: Throwable? = null) :
        EventHandlingError()

    /**
     * Unexpected error during event handling.
     */
    data class UnexpectedError(val message: String, override val occurredAt: Instant = SystemTimeProvider().now(), override val cause: Throwable? = null) :
        EventHandlingError()
}
