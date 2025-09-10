package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Value object representing metadata differences.
 */
data class MetadataDiff(
    val added: Map<String, String>,
    val removed: Set<String>,
    val modified: Map<String, Pair<String, String>>, // key -> (oldValue, newValue)
) {
    fun hasChanges(): Boolean = added.isNotEmpty() || removed.isNotEmpty() || modified.isNotEmpty()
}
