package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Result of multi-way conflict detection.
 */
data class MultiWayConflictResult(
    val conflicts: List<Conflict>,
    val changeSetCount: Int,
    val pairwiseResults: List<ConflictDetectionResult>,
    val canAutoMerge: Boolean,
)
