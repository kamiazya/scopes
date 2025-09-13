package io.github.kamiazya.scopes.collaborativeversioning.domain.dto

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.*

/**
 * Options for visualization.
 */
data class VisualizationOptions(
    val showValues: Boolean = true,
    val colors: DiffColors = DiffColors.default(),
    val maxValueLength: Int = 100,
    val contextLines: Int = 3,
) {
    companion object {
        fun default() = VisualizationOptions()

        fun forConflicts() = VisualizationOptions(
            showValues = true,
            colors = DiffColors.conflict(),
        )

        fun minimal() = VisualizationOptions(
            showValues = false,
            contextLines = 1,
        )
    }
}

/**
 * Color scheme for diff visualization.
 */
data class DiffColors(
    val added: String = "#28a745",
    val removed: String = "#dc3545",
    val modified: String = "#ffc107",
    val moved: String = "#17a2b8",
    val unchanged: String = "#6c757d",
) {
    companion object {
        fun default() = DiffColors()

        fun conflict() = DiffColors(
            added = "#ff6b6b",
            removed = "#ff6b6b",
            modified = "#ff9f40",
        )

        fun monochrome() = DiffColors(
            added = "#000000",
            removed = "#666666",
            modified = "#333333",
            moved = "#999999",
            unchanged = "#cccccc",
        )
    }
}

/**
 * Visual representation of a diff.
 */
data class VisualDiff(
    val changes: List<VisualChange>,
    val statistics: DiffStatistics,
    val affectedPaths: List<String>,
    val structuralChanges: List<VisualStructuralChange>,
)

/**
 * Visual representation of a single change.
 */
data class VisualChange(
    val type: DiffChangeType,
    val path: String,
    val oldValue: String? = null,
    val value: String? = null,
    val targetPath: String? = null,
    val displayText: String,
    val color: String,
)

/**
 * Statistics about a diff.
 */
data class DiffStatistics(val totalChanges: Int, val additions: Int, val deletions: Int, val modifications: Int, val moves: Int)

/**
 * Visual representation of structural changes.
 */
data class VisualStructuralChange(val type: String, val description: String, val details: Map<String, String>)

/**
 * Side-by-side diff view.
 */
data class SideBySideDiff(val lines: List<SideBySideLine>, val metadata: Map<String, String> = emptyMap())

/**
 * A line in side-by-side diff.
 */
data class SideBySideLine(val leftLine: LineInfo?, val rightLine: LineInfo?)

/**
 * Information about a line in diff.
 */
data class LineInfo(val number: Int, val content: String, val type: LineType, val highlights: List<TextHighlight> = emptyList())

/**
 * Text highlight range.
 */
data class TextHighlight(val start: Int, val end: Int)

/**
 * Unified diff view.
 */
data class UnifiedDiff(val lines: List<UnifiedLine>, val hunks: List<DiffHunk>)

/**
 * A line in unified diff.
 */
data class UnifiedLine(val type: LineType, val content: String, val path: String)

/**
 * A hunk in unified diff.
 */
data class DiffHunk(val lines: List<UnifiedLine>)

/**
 * Visual representation of conflicts.
 */
data class ConflictVisualization(val conflicts: List<VisualConflict>, val summary: ConflictSummary)

/**
 * Visual representation of a single conflict.
 */
data class VisualConflict(
    val id: String,
    val type: ConflictType,
    val severity: ConflictSeverity,
    val path: String,
    val description: String,
    val leftChange: VisualChange,
    val rightChange: VisualChange,
    val resolution: VisualResolution? = null,
)

/**
 * Visual representation of conflict resolution.
 */
data class VisualResolution(val type: VisualizationResolutionType, val description: String)

/**
 * Summary of conflicts.
 */
data class ConflictSummary(val total: Int, val bySeverity: Map<ConflictSeverity, Int>, val byType: Map<ConflictType, Int>, val resolvable: Int)
