package io.github.kamiazya.scopes.application.service.error

/**
 * Configuration errors for notification service setup failures.
 * These handle failures in notification service configuration.
 */
sealed class NotificationConfigurationError : NotificationServiceError() {
    
    /**
     * Channel configuration is invalid or unreachable.
     */
    data class InvalidChannelConfiguration(
        val channel: String,
        val configurationKey: String,
        val errorDetails: String,
        val requiredFields: List<String>
    ) : NotificationConfigurationError()
    
    /**
     * Notification preferences are invalid or conflicting.
     */
    data class InvalidPreferences(
        val userId: String,
        val preferenceKey: String,
        val conflict: String,
        val resolution: String?
    ) : NotificationConfigurationError()
}