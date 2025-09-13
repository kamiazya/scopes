package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Severity levels for conflicts.
 */
enum class ConflictSeverity {
    Low, // Can likely be auto-resolved
    Medium, // Requires review but has clear resolution
    High, // Requires manual intervention
}
