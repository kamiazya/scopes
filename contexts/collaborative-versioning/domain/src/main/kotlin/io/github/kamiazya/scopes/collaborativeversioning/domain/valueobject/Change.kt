package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Represents a single change within a changeset.
 */
data class Change(
    val path: String,
    val operation: ChangeOperation,
    val previousValue: String?,
    val newValue: String?,
    val metadata: Map<String, String> = emptyMap(),
)
