package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Configuration for conflict detection.
 */
data class ConflictDetectorConfig(
    val detectSemanticConflicts: Boolean = true,
    val detectStructuralConflicts: Boolean = true,
    val strictMode: Boolean = false,
) {
    companion object {
        fun default() = ConflictDetectorConfig()

        fun strict() = ConflictDetectorConfig(
            detectSemanticConflicts = true,
            detectStructuralConflicts = true,
            strictMode = true,
        )

        fun lenient() = ConflictDetectorConfig(
            detectSemanticConflicts = false,
            detectStructuralConflicts = true,
            strictMode = false,
        )
    }
}
