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
        val invalidVariableReasons: Map<String, String>,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : ConfigurationAdapterError() {
        // Environment configuration errors are persistent
        override val retryable: Boolean = false

        /**
         * Provides a sanitized representation of invalid variable reasons for safe logging.
         * Ensures that error reasons do not contain sensitive information.
         */
        val sanitizedReasons: Map<String, String>
            get() = invalidVariableReasons.mapValues { (_, reason) ->
                sanitizeErrorReason(reason)
            }

        /**
         * Custom toString that uses sanitized reasons to prevent exposure of sensitive data.
         */
        override fun toString(): String =
            "EnvironmentError(environment='$environment', missingVariables=$missingVariables, " +
            "invalidVariableReasons=$sanitizedReasons, timestamp=$timestamp, " +
            "correlationId=$correlationId, retryable=$retryable)"

        /**
         * Sanitizes error reason strings to prevent leakage of sensitive values.
         * This method follows the same redaction pattern as ValidationError.
         */
        private fun sanitizeErrorReason(reason: String): String {
            // Look for patterns like "got 'value'" or "got \"value\""
            val gotQuotedPattern = Regex("""(.* got ')([^']+)('.*)|(.* got ")([^"]+)(".*)|(.* got )([^'\s][^\s]*)(\s.*|$)""", RegexOption.IGNORE_CASE)
            val match = gotQuotedPattern.find(reason)

            return if (match != null) {
                val (prefix, value, suffix) = when {
                    match.groupValues[1].isNotEmpty() -> Triple(match.groupValues[1], match.groupValues[2], match.groupValues[3])
                    match.groupValues[4].isNotEmpty() -> Triple(match.groupValues[4], match.groupValues[5], match.groupValues[6])
                    match.groupValues[7].isNotEmpty() -> Triple(match.groupValues[7], match.groupValues[8], match.groupValues[9])
                    else -> return reason
                }
                val redactedValue = redactSensitiveValue(value)
                "$prefix$redactedValue$suffix"
            } else {
                // If no sensitive pattern is detected, return the reason as-is
                reason
            }
        }

        /**
         * Applies the same redaction logic as ValidationError.actualValueRedacted
         */
        private fun redactSensitiveValue(value: String): String = when {
            value.isBlank() -> "<blank>"
            value.length <= 4 -> "*".repeat(value.length)
            else -> "${value.take(2)}${"*".repeat(value.length - 4)}${value.takeLast(2)}"
        }
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
