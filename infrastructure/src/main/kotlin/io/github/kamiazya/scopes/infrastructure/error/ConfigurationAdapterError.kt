package io.github.kamiazya.scopes.infrastructure.error

import kotlinx.datetime.Instant

/**
 * Configuration adapter errors for settings management.
 * Covers loading, validation, and environment-specific issues.
 */
sealed class ConfigurationAdapterError : InfrastructureAdapterError() {
    
    /**
     * Configuration source loading errors.
     */
    data class LoadingError(
        val sourceType: ConfigurationType,
        val sourcePath: String,
        val cause: Throwable,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : ConfigurationAdapterError() {
        // Retry logic based on source type
        override val retryable: Boolean = sourceType.isRetryable()
    }
    
    /**
     * Configuration validation errors.
     */
    data class ValidationError(
        val configKey: String,
        val expectedType: String,
        val actualValue: String?,
        val validationRules: List<String>,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : ConfigurationAdapterError() {
        // Validation errors indicate persistent misconfigurations
        override val retryable: Boolean = false
    }
    
    /**
     * Environment-specific configuration errors.
     */
    data class EnvironmentError(
        val environment: String,
        val missingVariables: List<String>,
        val invalidVariables: Map<String, String>,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : ConfigurationAdapterError() {
        // Environment configuration errors are persistent
        override val retryable: Boolean = false
    }
    
    /**
     * Configuration encryption/decryption errors.
     */
    data class EncryptionError(
        val configKey: String,
        val operation: EncryptionOperation,
        val algorithm: String,
        val cause: Throwable,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : ConfigurationAdapterError() {
        // Encryption errors indicate persistent key/algorithm issues
        override val retryable: Boolean = false
    }
}