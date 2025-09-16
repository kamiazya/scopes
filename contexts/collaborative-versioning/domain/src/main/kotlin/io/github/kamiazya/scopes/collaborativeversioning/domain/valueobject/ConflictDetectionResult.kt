package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Result of conflict detection.
 */
data class ConflictDetectionResult(val conflicts: List<Conflict>, val canAutoMerge: Boolean, val metadata: Map<String, String> = emptyMap()) {
    fun hasConflicts(): Boolean = conflicts.isNotEmpty()

    fun conflictsByType(): Map<ConflictType, List<Conflict>> = conflicts.groupBy { it.type }

    fun highSeverityConflicts(): List<Conflict> = conflicts.filter { it.severity == ConflictSeverity.High }
}
