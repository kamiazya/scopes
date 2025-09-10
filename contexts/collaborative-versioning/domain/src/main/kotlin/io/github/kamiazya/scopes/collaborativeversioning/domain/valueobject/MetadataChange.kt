package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Represents a change in metadata.
 */
sealed class MetadataChange {
    data class Added(val value: String) : MetadataChange()
    data class Removed(val oldValue: String) : MetadataChange()
    data class Modified(val oldValue: String, val newValue: String) : MetadataChange()
}
