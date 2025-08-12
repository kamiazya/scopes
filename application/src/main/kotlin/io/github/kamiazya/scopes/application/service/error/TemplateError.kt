package io.github.kamiazya.scopes.application.service.error

/**
 * Template errors for message template processing failures.
 * These handle failures in template resolution and rendering.
 */
sealed class TemplateError : NotificationServiceError() {

    /**
     * Required message template was not found.
     */
    data class TemplateNotFound(
        val templateId: String,
        val locale: String?,
        val searchPaths: List<String>
    ) : TemplateError()

    /**
     * Template rendering failed due to missing or invalid variables.
     */
    data class TemplateRenderingFailure(
        val templateId: String,
        val missingVariables: List<String>,
        val invalidVariableReasons: Map<String, String>,
        val cause: Throwable?
    ) : TemplateError() {

        /**
         * Provides a sanitized representation of invalid variable reasons for safe logging.
         * Ensures that error reasons do not contain sensitive template variable values.
         */
        val sanitizedReasons: Map<String, String>
            get() = invalidVariableReasons.mapValues { (_, reason) ->
                sanitizeTemplateReason(reason)
            }

        /**
         * Custom toString that uses sanitized reasons to prevent exposure of sensitive data.
         */
        override fun toString(): String =
            "TemplateRenderingFailure(templateId='$templateId', missingVariables=$missingVariables, " +
            "invalidVariableReasons=$sanitizedReasons, cause=$cause::class.simpleName)"

        /**
         * Sanitizes template error reason strings to prevent leakage of sensitive values.
         * Similar pattern to ConfigurationAdapterError.EnvironmentError sanitization.
         */
        private fun sanitizeTemplateReason(reason: String): String {
            // Pattern to match error messages that contain actual variable values
            val valuePattern = Regex("""(.* value ')([^']+)('.*)|(.* value ")([^"]+)(".*)|(.* value )([^'\s][^\s]*)(\s.*|$)""", RegexOption.IGNORE_CASE)
            val gotPattern = Regex("""(.* got ')([^']+)('.*)|(.* got ")([^"]+)(".*)|(.* got )([^'\s][^\s]*)(\s.*|$)""", RegexOption.IGNORE_CASE)

            return when {
                valuePattern.find(reason) != null -> {
                    val match = valuePattern.find(reason)!!
                    val (prefix, value, suffix) = when {
                        match.groupValues[1].isNotEmpty() -> Triple(match.groupValues[1], match.groupValues[2], match.groupValues[3])
                        match.groupValues[4].isNotEmpty() -> Triple(match.groupValues[4], match.groupValues[5], match.groupValues[6])
                        match.groupValues[7].isNotEmpty() -> Triple(match.groupValues[7], match.groupValues[8], match.groupValues[9])
                        else -> return reason
                    }
                    val redactedValue = redactSensitiveValue(value)
                    "$prefix$redactedValue$suffix"
                }
                gotPattern.find(reason) != null -> {
                    val match = gotPattern.find(reason)!!
                    val (prefix, value, suffix) = when {
                        match.groupValues[1].isNotEmpty() -> Triple(match.groupValues[1], match.groupValues[2], match.groupValues[3])
                        match.groupValues[4].isNotEmpty() -> Triple(match.groupValues[4], match.groupValues[5], match.groupValues[6])
                        match.groupValues[7].isNotEmpty() -> Triple(match.groupValues[7], match.groupValues[8], match.groupValues[9])
                        else -> return reason
                    }
                    val redactedValue = redactSensitiveValue(value)
                    "$prefix$redactedValue$suffix"
                }
                else -> reason  // If no sensitive pattern is detected, return as-is
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
     * Template syntax is invalid.
     */
    data class InvalidTemplateSyntax(
        val templateId: String,
        val syntaxError: String,
        val lineNumber: Int?,
        val columnNumber: Int?
    ) : TemplateError()
}
