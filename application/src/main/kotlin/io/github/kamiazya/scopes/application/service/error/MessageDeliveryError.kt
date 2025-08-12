package io.github.kamiazya.scopes.application.service.error

import kotlinx.datetime.Instant

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
        val channel: String,
        val recipient: String,
        val attemptCount: Int,
        val lastError: String
    ) : MessageDeliveryError()
    
    /**
     * Channel is temporarily unavailable for message delivery.
     */
    data class ChannelUnavailable(
        val channel: String,
        val reason: String,
        val expectedRecoveryAt: Instant?,
        val alternativeChannels: List<String>
    ) : MessageDeliveryError()
    
    /**
     * Rate limit exceeded for notification channel.
     */
    data class RateLimitExceeded(
        val channel: String,
        val limit: Int,
        val windowSeconds: Int,
        val resetAt: Instant
    ) : MessageDeliveryError()
}