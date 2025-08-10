package io.github.kamiazya.scopes.application.service.error

/**
 * Notification service errors for message delivery and event distribution failures.
 * 
 * This hierarchy provides comprehensive error types for notification concerns
 * including message delivery, event distribution, template processing, and configuration issues.
 * 
 * Based on Serena MCP research on notification patterns:
 * - Message delivery failure handling with retry mechanisms
 * - Event sourcing and distribution error management
 * - Template processing and rendering failures
 * - Channel-specific configuration and credential errors
 * 
 * Following functional error handling principles for composability and recovery.
 */
sealed class NotificationServiceError {

    /**
     * Message delivery errors for communication channel failures.
     * These represent failures in delivering notifications through various channels.
     */
    sealed class MessageDeliveryError : NotificationServiceError() {
        
        /**
         * Message delivery failed after multiple attempts.
         */
        data class DeliveryFailure(
            val messageId: String,
            val recipient: String,
            val channel: String,
            val attemptCount: Int,
            val lastFailureReason: String
        ) : MessageDeliveryError()
        
        /**
         * Recipient address or identifier is invalid for the channel.
         */
        data class InvalidRecipient(
            val recipient: String,
            val channel: String,
            val validationError: String,
            val messageId: String
        ) : MessageDeliveryError()
        
        /**
         * Communication channel is currently unavailable.
         */
        data class ChannelUnavailable(
            val channel: String,
            val reason: String,
            val estimatedRecoveryTime: Long,
            val messageId: String
        ) : MessageDeliveryError()
    }

    /**
     * Event distribution errors for domain event publishing failures.
     * These handle errors in distributing events to subscribers and services.
     */
    sealed class EventDistributionError : NotificationServiceError() {
        
        /**
         * Failed to publish event to one or more subscribers.
         */
        data class EventPublishingFailure(
            val eventId: String,
            val eventType: String,
            val targetSubscribers: List<String>,
            val failedSubscribers: List<String>,
            val cause: Throwable
        ) : EventDistributionError()
        
        /**
         * Subscriber took too long to process the event.
         */
        data class SubscriberTimeout(
            val subscriberId: String,
            val eventId: String,
            val timeoutMs: Long,
            val elapsedMs: Long
        ) : EventDistributionError()
        
        /**
         * Event format is invalid for the subscriber.
         */
        data class InvalidEventFormat(
            val eventId: String,
            val eventType: String,
            val formatError: String,
            val subscriberId: String
        ) : EventDistributionError()
    }

    /**
     * Template processing errors for message rendering failures.
     * These handle failures in template resolution and rendering.
     */
    sealed class TemplateError : NotificationServiceError() {
        
        /**
         * Required message template was not found.
         */
        data class TemplateNotFound(
            val templateId: String,
            val channel: String,
            val availableTemplates: List<String>,
            val messageId: String
        ) : TemplateError()
        
        /**
         * Template rendering failed due to data or syntax issues.
         */
        data class TemplateRenderingFailure(
            val templateId: String,
            val messageId: String,
            val renderingError: String,
            val templateData: Map<String, Any>
        ) : TemplateError()
        
        /**
         * Template version conflict or incompatibility.
         */
        data class TemplateVersionConflict(
            val templateId: String,
            val requestedVersion: String,
            val availableVersion: String,
            val messageId: String
        ) : TemplateError()
    }

    /**
     * Configuration errors for service setup and credential issues.
     * These handle failures in notification service configuration.
     */
    sealed class ConfigurationError : NotificationServiceError() {
        
        /**
         * Channel configuration is invalid or unreachable.
         */
        data class InvalidChannelConfiguration(
            val channel: String,
            val configurationKey: String,
            val configurationError: String,
            val affectedMessages: List<String>
        ) : ConfigurationError()
        
        /**
         * Required credentials are missing for the channel.
         */
        data class MissingCredentials(
            val channel: String,
            val credentialType: String,
            val service: String
        ) : ConfigurationError()
    }
}