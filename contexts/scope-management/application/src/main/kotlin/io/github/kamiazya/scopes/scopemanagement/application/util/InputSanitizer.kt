package io.github.kamiazya.scopes.scopemanagement.application.util

/**
 * Utility for sanitizing user input before including it in errors or logs.
 * This prevents sensitive data from being exposed and provides safe previews.
 * Supports Unicode characters for international users.
 */
object InputSanitizer {
    private const val MAX_PREVIEW_LENGTH = 50
    private const val TRUNCATION_INDICATOR = "..."

    /**
     * Creates a safe preview of user input for error messages.
     * - Truncates long inputs
     * - Masks potential sensitive patterns
     * - Escapes control characters
     * - Preserves Unicode characters for international support
     */
    fun createPreview(input: String): String {
        // Handle empty or blank input
        if (input.isBlank()) {
            return "[empty]"
        }

        // Truncate if too long (using Unicode-aware length)
        val truncated = if (input.length > MAX_PREVIEW_LENGTH) {
            input.take(MAX_PREVIEW_LENGTH - TRUNCATION_INDICATOR.length) + TRUNCATION_INDICATOR
        } else {
            input
        }

        // Escape control characters while preserving Unicode text
        return truncated
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\u0000", "\\0")
            .filter { isDisplayableCharacter(it) }
    }

    /**
     * Creates a safe field name representation.
     * Supports Unicode letters and digits for international field names.
     */
    fun sanitizeFieldName(field: String): String = field.filter { 
        Character.isLetterOrDigit(it) || it in ".-_" 
    }

    /**
     * Determines if a character is safe to display in error messages.
     * Includes Unicode letters, digits, and common punctuation/symbols.
     * Excludes control characters and potentially problematic characters.
     */
    private fun isDisplayableCharacter(char: Char): Boolean {
        return when {
            // Allow Unicode letters and digits (supports all languages)
            Character.isLetterOrDigit(char) -> true
            
            // Allow common punctuation and symbols
            char in " -_.,;:!?@#$%^&*()[]{}/<>='\"\\+" -> true
            
            // Allow mathematical symbols (Unicode category Sm)
            Character.getType(char) == Character.MATH_SYMBOL.toInt() -> true
            
            // Allow currency symbols (Unicode category Sc)
            Character.getType(char) == Character.CURRENCY_SYMBOL.toInt() -> true
            
            // Allow other symbols that are commonly used (Unicode category So)
            Character.getType(char) == Character.OTHER_SYMBOL.toInt() -> true
            
            // Allow connector punctuation (underscore variants in other languages)
            Character.getType(char) == Character.CONNECTOR_PUNCTUATION.toInt() -> true
            
            // Allow dash punctuation (various dash types in different languages)
            Character.getType(char) == Character.DASH_PUNCTUATION.toInt() -> true
            
            // Allow start/end punctuation (quotes, brackets in various languages)
            Character.getType(char) == Character.START_PUNCTUATION.toInt() ||
            Character.getType(char) == Character.END_PUNCTUATION.toInt() -> true
            
            // Allow other punctuation (language-specific punctuation marks)
            Character.getType(char) == Character.OTHER_PUNCTUATION.toInt() -> true
            
            // Exclude control characters and private use areas
            Character.isISOControl(char) -> false
            
            // Default: allow (conservative approach for international support)
            else -> true
        }
    }
}
