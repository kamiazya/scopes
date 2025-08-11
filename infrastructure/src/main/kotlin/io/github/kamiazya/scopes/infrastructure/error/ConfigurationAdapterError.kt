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
    class ValidationError(
        val configKey: String,
        val expectedType: String,
        private val actualValue: String?,
        val validationRules: List<String>,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : ConfigurationAdapterError() {
        // Validation errors indicate persistent misconfigurations
        override val retryable: Boolean = false

        /**
         * Provides a redacted representation of the actual value for safe logging.
         * Prevents potential leakage of sensitive configuration values.
         */
        val actualValueRedacted: String
            get() = when {
                actualValue == null -> "<null>"
                actualValue.isBlank() -> "<blank>"
                actualValue.length <= 4 -> "*".repeat(actualValue.length)
                else -> "${actualValue.take(2)}${"*".repeat(actualValue.length - 4)}${actualValue.takeLast(2)}"
            }

        /**
         * Custom toString that never exposes the raw actualValue.
         */
        override fun toString(): String =
            "ValidationError(configKey='$configKey', expectedType='$expectedType', " +
            "actualValue='$actualValueRedacted', validationRules=$validationRules, " +
            "timestamp=$timestamp, correlationId=$correlationId, retryable=$retryable)"

        /**
         * Custom equals that excludes actualValue from comparison to prevent
         * accidental exposure through object comparison.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ValidationError) return false

            return configKey == other.configKey &&
                expectedType == other.expectedType &&
                validationRules == other.validationRules &&
                timestamp == other.timestamp &&
                correlationId == other.correlationId
        }

        /**
         * Custom hashCode that excludes actualValue to maintain security.
         */
        override fun hashCode(): Int {
            var result = configKey.hashCode()
            result = 31 * result + expectedType.hashCode()
            result = 31 * result + validationRules.hashCode()
            result = 31 * result + timestamp.hashCode()
            result = 31 * result + (correlationId?.hashCode() ?: 0)
            return result
        }
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
