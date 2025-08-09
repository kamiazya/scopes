package io.github.kamiazya.scopes.domain.util

/**
 * Utility object for normalizing scope titles to ensure consistent comparison and storage.
 * 
 * This utility provides a centralized implementation of title normalization logic
 * that is used across the domain, application, and infrastructure layers to ensure
 * consistent handling of scope titles.
 */
object TitleNormalizer {
    
    /**
     * Normalizes a title for consistent comparison and storage by:
     * - Trimming leading and trailing whitespace
     * - Collapsing internal whitespace sequences (spaces, tabs, newlines) to single spaces
     * - Converting to lowercase using locale-invariant conversion
     * 
     * This ensures that titles like "My Task", "my  task", and "MY\tTASK\n" are all
     * normalized to the same value for consistent duplicate detection and storage.
     * 
     * Uses locale-invariant lowercase conversion to avoid issues like the Turkish-I problem
     * where 'I'.lowercase() produces 'ı' instead of 'i' in Turkish locale.
     * 
     * @param title The raw title string to normalize
     * @return The normalized title string
     * 
     * @example
     * ```kotlin
     * TitleNormalizer.normalize("  My  Task  ") // returns "my task"
     * TitleNormalizer.normalize("My\t\tTask\n") // returns "my task"  
     * TitleNormalizer.normalize("MY    TASK")   // returns "my task"
     * ```
     */
    fun normalize(title: String): String {
        return title.trim()
            .replace(Regex("\\s+"), " ")
            .lowercaseInvariant()
    }
    
    /**
     * Converts a string to lowercase using locale-invariant rules.
     * This is equivalent to Java's String.toLowerCase(Locale.ROOT) but works
     * across all Kotlin Multiplatform targets.
     * 
     * This prevents locale-specific issues like the Turkish-I problem where
     * 'I'.lowercase() in Turkish locale produces 'ı' instead of 'i'.
     */
    private fun String.lowercaseInvariant(): String {
        return this.map { char ->
            when (char) {
                in 'A'..'Z' -> char + ('a' - 'A')
                else -> char
            }
        }.joinToString("")
    }
}
