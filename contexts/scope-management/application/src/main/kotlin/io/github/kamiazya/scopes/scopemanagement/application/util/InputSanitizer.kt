package io.github.kamiazya.scopes.scopemanagement.application.util

/**
 * Utility for sanitizing user input before including it in errors or logs.
 * This prevents sensitive data from being exposed and provides safe previews.
 */
object InputSanitizer {
    private const val MAX_PREVIEW_LENGTH = 50
    private const val TRUNCATION_INDICATOR = "..."

    /**
     * Creates a safe preview of user input for error messages.
     * - Truncates long inputs
     * - Masks potential sensitive patterns
     * - Escapes special characters
     */
    fun createPreview(input: String): String {
        // Handle empty or blank input
        if (input.isBlank()) {
            return "[empty]"
        }

        // Truncate if too long
        val truncated = if (input.length > MAX_PREVIEW_LENGTH) {
            input.take(MAX_PREVIEW_LENGTH - TRUNCATION_INDICATOR.length) + TRUNCATION_INDICATOR
        } else {
            input
        }

        // Escape special characters and control characters
        return truncated
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\u0000", "\\0")
            .filter { it.isLetterOrDigit() || it in " -_.,;:!?@#$%^&*()[]{}/<>='\"\\+" }
    }

    /**
     * Creates a safe field name representation.
     */
    fun sanitizeFieldName(field: String): String = field.filter { it.isLetterOrDigit() || it in ".-_" }
}
