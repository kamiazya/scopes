package io.github.kamiazya.scopes.infrastructure.error

import kotlinx.datetime.Instant

/**
 * Messaging adapter errors for message broker operations.
 * Covers message delivery, serialization, and broker connectivity.
 */
sealed class MessagingAdapterError : InfrastructureAdapterError() {
    
    /**
     * Message broker connection errors.
     */
    data class BrokerConnectionError(
        val brokerUrl: String,
        val connectionType: ConnectionType,
        val cause: Throwable,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : MessagingAdapterError() {
        // Broker connection issues are transient
        override val retryable: Boolean = true
    }
    
    /**
     * Message serialization/deserialization errors.
     */
    data class SerializationError(
        val messageId: String,
        val operation: SerializationOperation,
        val format: String,
        val cause: Throwable,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : MessagingAdapterError() {
        // Serialization errors indicate data format issues, not retryable
        override val retryable: Boolean = false
    }
    
    /**
     * Message delivery failures.
     */
    data class DeliveryError(
        val messageId: String,
        val destination: String,
        val attemptCount: Int,
        val maxRetries: Int,
        val cause: Throwable? = null,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : MessagingAdapterError() {
        // Retry only if we haven't exceeded the maximum retry limit
        override val retryable: Boolean = attemptCount < maxRetries
    }
    
    /**
     * Queue/topic capacity errors.
     */
    data class CapacityError(
        val queueName: String,
        val capacityType: QueueCapacityType,
        val currentSize: Long,
        val maxSize: Long,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : MessagingAdapterError() {
        // Message count limits may clear, but size limits are persistent
        override val retryable: Boolean = capacityType == QueueCapacityType.MESSAGE_COUNT
    }
}