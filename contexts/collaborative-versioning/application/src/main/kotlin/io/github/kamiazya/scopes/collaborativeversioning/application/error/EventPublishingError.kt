package io.github.kamiazya.scopes.collaborativeversioning.application.error

import io.github.kamiazya.scopes.collaborativeversioning.domain.service.SystemTimeProvider
import io.github.kamiazya.scopes.platform.application.error.ApplicationError
import io.github.kamiazya.scopes.platform.domain.value.EventId
import kotlinx.datetime.Instant

/**
 * Errors that can occur when publishing domain events.
 */
sealed class EventPublishingError : ApplicationError {
    abstract override val occurredAt: Instant
    abstract override val cause: Throwable?

    /**
     * Failed to serialize the event for storage.
     */
    data class SerializationFailed(
        val eventId: EventId,
        val eventType: String,
        val reason: String,
        override val occurredAt: Instant = SystemTimeProvider().now(),
        override val cause: Throwable? = null,
    ) : EventPublishingError()

    /**
     * Failed to store the event in the event store.
     */
    data class StorageFailed(
        val eventId: EventId,
        val eventType: String,
        val reason: String,
        override val occurredAt: Instant = SystemTimeProvider().now(),
        override val cause: Throwable? = null,
    ) : EventPublishingError()

    /**
     * Failed to publish the event to subscribers.
     */
    data class DistributionFailed(
        val eventId: EventId,
        val eventType: String,
        val reason: String,
        val failedSubscribers: List<String> = emptyList(),
        override val occurredAt: Instant = SystemTimeProvider().now(),
        override val cause: Throwable? = null,
    ) : EventPublishingError()

    /**
     * Event type is not registered in the event store.
     */
    data class UnregisteredEventType(
        val eventType: String,
        val eventClass: String,
        override val occurredAt: Instant = SystemTimeProvider().now(),
        override val cause: Throwable? = null,
    ) : EventPublishingError()

    /**
     * Timeout occurred while publishing the event.
     */
    data class PublishTimeout(
        val eventId: EventId,
        val eventType: String,
        val timeoutMs: Long,
        override val occurredAt: Instant = SystemTimeProvider().now(),
        override val cause: Throwable? = null,
    ) : EventPublishingError()

    /**
     * Infrastructure error during event publishing.
     */
    data class InfrastructureError(val message: String, override val occurredAt: Instant = SystemTimeProvider().now(), override val cause: Throwable? = null) :
        EventPublishingError()
}
