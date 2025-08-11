package io.github.kamiazya.scopes.application.service.error

/**
 * Event distribution errors for event bus and subscription failures.
 * These handle errors in distributing events to subscribers and services.
 */
sealed class EventDistributionError : NotificationServiceError() {
    
    /**
     * Failed to publish event to one or more subscribers.
     */
    data class EventPublishingFailure(
        val eventId: String,
        val eventType: String,
        val failedSubscribers: List<String>,
        val successfulSubscribers: List<String>,
        val cause: Throwable
    ) : EventDistributionError()
    
    /**
     * Subscriber endpoint is not responding.
     */
    data class SubscriberUnreachable(
        val subscriberId: String,
        val endpoint: String,
        val lastAttempt: Long,
        val retryCount: Int
    ) : EventDistributionError()
    
    /**
     * Event routing configuration is invalid.
     */
    data class InvalidRoutingConfiguration(
        val eventType: String,
        val configurationError: String,
        val affectedRoutes: List<String>
    ) : EventDistributionError()
}