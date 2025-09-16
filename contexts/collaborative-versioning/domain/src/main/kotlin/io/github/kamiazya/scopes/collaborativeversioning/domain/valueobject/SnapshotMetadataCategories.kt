package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Value object categorizing snapshot metadata into different categories.
 */
data class SnapshotMetadataCategories(
    val system: Map<String, String>,
    val user: Map<String, String>,
    val performance: Map<String, String>,
    val audit: Map<String, String>,
) {
    val all: Map<String, String> = system + user + performance + audit
}
