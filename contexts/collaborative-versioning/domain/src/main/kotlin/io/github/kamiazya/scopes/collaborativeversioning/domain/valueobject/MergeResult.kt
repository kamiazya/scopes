package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import kotlinx.serialization.json.JsonElement

/**
 * Result of a merge operation.
 */
data class MergeResult(
    val mergedDocument: JsonElement,
    val mergedChangeSet: ChangeSet,
    val resolvedConflicts: List<Conflict>,
    val unresolvedConflicts: List<Conflict>,
    val metadata: Map<String, String> = emptyMap(),
) {
    fun isSuccessful(): Boolean = unresolvedConflicts.isEmpty()

    fun hasUnresolvedConflicts(): Boolean = unresolvedConflicts.isNotEmpty()

    fun resolutionRate(): Double {
        val total = resolvedConflicts.size + unresolvedConflicts.size
        return if (total > 0) {
            resolvedConflicts.size.toDouble() / total
        } else {
            1.0
        }
    }
}
