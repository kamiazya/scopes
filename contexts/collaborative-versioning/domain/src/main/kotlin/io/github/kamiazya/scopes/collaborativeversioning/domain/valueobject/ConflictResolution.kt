package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Suggested resolution for a conflict.
 */
sealed class ConflictResolution {
    data class Automatic(val strategy: String, val description: String) : ConflictResolution()

    data class Manual(val suggestion: String) : ConflictResolution()
}
