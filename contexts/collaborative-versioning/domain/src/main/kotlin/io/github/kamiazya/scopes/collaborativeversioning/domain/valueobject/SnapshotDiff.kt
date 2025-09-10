package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Value object representing the differences between two snapshots.
 */
data class SnapshotDiff(
    val fromSnapshotId: SnapshotId,
    val toSnapshotId: SnapshotId,
    val contentDiff: ContentDiff,
    val metadataChanges: Map<String, MetadataChange>,
    val sizeChange: SizeChange,
) {
    /**
     * Check if there are any changes between the snapshots.
     */
    fun hasChanges(): Boolean = contentDiff.hasChanges() || metadataChanges.isNotEmpty()
}
