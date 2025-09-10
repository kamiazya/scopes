package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Value object representing content differences between two states.
 */
data class ContentDiff(val operations: List<DiffOperation>, val addedPaths: Set<String>, val removedPaths: Set<String>, val modifiedPaths: Set<String>) {
    /**
     * Check if there are any content changes.
     */
    fun hasChanges(): Boolean = operations.isNotEmpty()

    /**
     * Get total number of changed paths.
     */
    fun totalChangedPaths(): Int = addedPaths.size + removedPaths.size + modifiedPaths.size

    /**
     * Get the total number of changes.
     */
    fun changeCount(): Int = addedPaths.size + removedPaths.size + modifiedPaths.size
}
