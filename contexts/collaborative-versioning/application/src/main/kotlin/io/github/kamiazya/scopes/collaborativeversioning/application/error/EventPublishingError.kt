package io.github.kamiazya.scopes.collaborativeversioning.application.error

import io.github.kamiazya.scopes.platform.domain.value.EventId

/**
 * Errors that can occur when publishing domain events.
 */
sealed class EventPublishingError(recoverable: Boolean = true) : ApplicationError(recoverable) {
    /**
     * Failed to serialize the event for storage.
     */
    data class SerializationFailed(val eventId: EventId, val eventType: String, val reason: String) : EventPublishingError()

    /**
     * Failed to store the event in the event store.
     */
    data class StorageFailed(val eventId: EventId, val eventType: String, val reason: String) : EventPublishingError(recoverable = false)

    /**
     * Failed to publish the event to subscribers.
     */
    data class DistributionFailed(val eventId: EventId, val eventType: String, val reason: String, val failedSubscribers: List<String> = emptyList()) :
        EventPublishingError()

    /**
     * Event type is not registered in the event store.
     */
    data class UnregisteredEventType(val eventType: String, val eventClass: String) : EventPublishingError()

    /**
     * Timeout occurred while publishing the event.
     */
    data class PublishTimeout(val eventId: EventId, val eventType: String, val timeoutMs: Long) : EventPublishingError()

    /**
     * Infrastructure error during event publishing.
     */
    data class InfrastructureError(val message: String) : EventPublishingError(recoverable = false)
}
