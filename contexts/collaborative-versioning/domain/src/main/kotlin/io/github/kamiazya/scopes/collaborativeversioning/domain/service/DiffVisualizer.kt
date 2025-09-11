package io.github.kamiazya.scopes.collaborativeversioning.domain.service

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.collaborativeversioning.domain.dto.*
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.JsonDiffError
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.*
import kotlinx.serialization.json.*

/**
 * Service for creating visual representations of diffs for UI display.
 */
interface DiffVisualizer {
    /**
     * Create a visual diff representation.
     *
     * @param diff The diff to visualize
     * @param options Visualization options
     * @return Either an error or the visual diff
     */
    fun visualize(diff: JsonDiff, options: VisualizationOptions = VisualizationOptions.default()): Either<JsonDiffError, VisualDiff>

    /**
     * Create a side-by-side diff view.
     *
     * @param source The source document
     * @param target The target document
     * @param diff The diff between them
     * @return Either an error or the side-by-side view
     */
    fun createSideBySideView(source: JsonElement, target: JsonElement, diff: JsonDiff): Either<JsonDiffError, SideBySideDiff>

    /**
     * Create a unified diff view.
     *
     * @param source The source document
     * @param diff The diff to apply
     * @return Either an error or the unified view
     */
    fun createUnifiedView(source: JsonElement, diff: JsonDiff): Either<JsonDiffError, UnifiedDiff>

    /**
     * Create a conflict visualization.
     *
     * @param conflicts The conflicts to visualize
     * @param base The base document
     * @return Either an error or the conflict visualization
     */
    fun visualizeConflicts(conflicts: List<Conflict>, base: JsonElement): Either<JsonDiffError, ConflictVisualization>
}

/**
 * Default implementation of DiffVisualizer.
 */
class DefaultDiffVisualizer : DiffVisualizer {

    override fun visualize(diff: JsonDiff, options: VisualizationOptions): Either<JsonDiffError, VisualDiff> = either {
        val visualChanges = diff.changes.map { change ->
            visualizeChange(change, options)
        }

        val statistics = DiffStatistics(
            totalChanges = diff.changeCount(),
            additions = diff.changesByType<JsonChange.Add>().size,
            deletions = diff.changesByType<JsonChange.Remove>().size,
            modifications = diff.changesByType<JsonChange.Replace>().size,
            moves = diff.changesByType<JsonChange.Move>().size,
        )

        VisualDiff(
            changes = visualChanges,
            statistics = statistics,
            affectedPaths = diff.affectedPaths().map { it.toString() },
            structuralChanges = diff.structuralChanges.map { change ->
                visualizeStructuralChange(change)
            },
        )
    }

    override fun createSideBySideView(source: JsonElement, target: JsonElement, diff: JsonDiff): Either<JsonDiffError, SideBySideDiff> = either {
        val sourceLines = formatJson(source).lines()
        val targetLines = formatJson(target).lines()

        val diffLines = mutableListOf<SideBySideLine>()
        var sourceIndex = 0
        var targetIndex = 0

        // Simple line-by-line comparison
        while (sourceIndex < sourceLines.size || targetIndex < targetLines.size) {
            val sourceLine = sourceLines.getOrNull(sourceIndex)
            val targetLine = targetLines.getOrNull(targetIndex)

            when {
                sourceLine == targetLine -> {
                    diffLines.add(
                        SideBySideLine(
                            leftLine = LineInfo(
                                number = sourceIndex + 1,
                                content = sourceLine ?: "",
                                type = LineType.Unchanged,
                            ),
                            rightLine = LineInfo(
                                number = targetIndex + 1,
                                content = targetLine ?: "",
                                type = LineType.Unchanged,
                            ),
                        ),
                    )
                    sourceIndex++
                    targetIndex++
                }
                sourceLine != null && targetLine == null -> {
                    diffLines.add(
                        SideBySideLine(
                            leftLine = LineInfo(
                                number = sourceIndex + 1,
                                content = sourceLine,
                                type = LineType.Removed,
                            ),
                            rightLine = null,
                        ),
                    )
                    sourceIndex++
                }
                sourceLine == null && targetLine != null -> {
                    diffLines.add(
                        SideBySideLine(
                            leftLine = null,
                            rightLine = LineInfo(
                                number = targetIndex + 1,
                                content = targetLine,
                                type = LineType.Added,
                            ),
                        ),
                    )
                    targetIndex++
                }
                else -> {
                    // Lines are different
                    diffLines.add(
                        SideBySideLine(
                            leftLine = LineInfo(
                                number = sourceIndex + 1,
                                content = sourceLine ?: "",
                                type = LineType.Modified,
                                highlights = findDifferences(sourceLine ?: "", targetLine ?: ""),
                            ),
                            rightLine = LineInfo(
                                number = targetIndex + 1,
                                content = targetLine ?: "",
                                type = LineType.Modified,
                                highlights = findDifferences(targetLine ?: "", sourceLine ?: ""),
                            ),
                        ),
                    )
                    sourceIndex++
                    targetIndex++
                }
            }
        }

        SideBySideDiff(
            lines = diffLines,
            metadata = mapOf(
                "sourceLines" to sourceLines.size.toString(),
                "targetLines" to targetLines.size.toString(),
            ),
        )
    }

    override fun createUnifiedView(source: JsonElement, diff: JsonDiff): Either<JsonDiffError, UnifiedDiff> = either {
        val sourceLines = formatJson(source).lines()
        val unifiedLines = mutableListOf<UnifiedLine>()

        // Apply diff to create unified view
        diff.changes.groupBy { change ->
            // Group changes by their top-level path
            val changePath = when (change) {
                is JsonChange.Add -> change.path
                is JsonChange.Remove -> change.path
                is JsonChange.Replace -> change.path
                is JsonChange.Move -> change.from
            }
            when (val firstSegment = changePath.segments.firstOrNull()) {
                is JsonPath.PathSegment.Field -> firstSegment.name
                is JsonPath.PathSegment.Index -> "[$firstSegment.value]"
                null -> "root"
            }
        }.forEach { (_, changes) ->
            // Process each group of changes
            changes.forEach { change ->
                when (change) {
                    is JsonChange.Add -> {
                        unifiedLines.add(
                            UnifiedLine(
                                type = LineType.Added,
                                content = "+ ${formatValue(change.value)}",
                                path = change.path.toString(),
                            ),
                        )
                    }
                    is JsonChange.Remove -> {
                        unifiedLines.add(
                            UnifiedLine(
                                type = LineType.Removed,
                                content = "- ${formatValue(change.oldValue)}",
                                path = change.path.toString(),
                            ),
                        )
                    }
                    is JsonChange.Replace -> {
                        unifiedLines.add(
                            UnifiedLine(
                                type = LineType.Removed,
                                content = "- ${formatValue(change.oldValue)}",
                                path = change.path.toString(),
                            ),
                        )
                        unifiedLines.add(
                            UnifiedLine(
                                type = LineType.Added,
                                content = "+ ${formatValue(change.newValue)}",
                                path = change.path.toString(),
                            ),
                        )
                    }
                    is JsonChange.Move -> {
                        unifiedLines.add(
                            UnifiedLine(
                                type = LineType.Context,
                                content = "~ Move from ${change.from} to ${change.to}",
                                path = change.from.toString(),
                            ),
                        )
                    }
                }
            }
        }

        UnifiedDiff(
            lines = unifiedLines,
            hunks = groupIntoHunks(unifiedLines),
        )
    }

    override fun visualizeConflicts(conflicts: List<Conflict>, base: JsonElement): Either<JsonDiffError, ConflictVisualization> = either {
        val visualConflicts = conflicts.map { conflict ->
            VisualConflict(
                id = conflict.id.toString(),
                type = conflict.type,
                severity = conflict.severity,
                path = conflict.path.toString(),
                description = conflict.description,
                leftChange = visualizeChange(conflict.change1, VisualizationOptions.forConflicts()),
                rightChange = visualizeChange(conflict.change2, VisualizationOptions.forConflicts()),
                resolution = conflict.resolution?.let { resolution ->
                    VisualResolution(
                        type = when (resolution) {
                            is ConflictResolution.Automatic -> VisualizationResolutionType.Automatic
                            is ConflictResolution.Manual -> VisualizationResolutionType.Manual
                        },
                        description = when (resolution) {
                            is ConflictResolution.Automatic -> resolution.description
                            is ConflictResolution.Manual -> resolution.suggestion
                        },
                    )
                },
            )
        }

        ConflictVisualization(
            conflicts = visualConflicts,
            summary = ConflictSummary(
                total = conflicts.size,
                bySeverity = conflicts.groupBy { it.severity }.mapValues { it.value.size },
                byType = conflicts.groupBy { it.type }.mapValues { it.value.size },
                resolvable = conflicts.count { it.resolution != null },
            ),
        )
    }

    private fun visualizeChange(change: JsonChange, options: VisualizationOptions): VisualChange = when (change) {
        is JsonChange.Add -> VisualChange(
            type = DiffChangeType.Add,
            path = change.path.toString(),
            value = if (options.showValues) formatValue(change.value) else null,
            displayText = "Added ${describePath(change.path)}",
            color = options.colors.added,
        )
        is JsonChange.Remove -> VisualChange(
            type = DiffChangeType.Remove,
            path = change.path.toString(),
            oldValue = if (options.showValues) formatValue(change.oldValue) else null,
            displayText = "Removed ${describePath(change.path)}",
            color = options.colors.removed,
        )
        is JsonChange.Replace -> VisualChange(
            type = DiffChangeType.Modify,
            path = change.path.toString(),
            oldValue = if (options.showValues) formatValue(change.oldValue) else null,
            value = if (options.showValues) formatValue(change.newValue) else null,
            displayText = "Modified ${describePath(change.path)}",
            color = options.colors.modified,
        )
        is JsonChange.Move -> VisualChange(
            type = DiffChangeType.Move,
            path = change.from.toString(),
            targetPath = change.to.toString(),
            value = if (options.showValues) formatValue(change.value) else null,
            displayText = "Moved from ${describePath(change.from)} to ${describePath(change.to)}",
            color = options.colors.moved,
        )
    }

    private fun visualizeStructuralChange(change: StructuralChange): VisualStructuralChange = when (change) {
        is StructuralChange.FieldAdded -> VisualStructuralChange(
            type = "field_added",
            description = "Field added: ${change.path}",
            details = mapOf("path" to change.path),
        )
        is StructuralChange.FieldRemoved -> VisualStructuralChange(
            type = "field_removed",
            description = "Field removed: ${change.path}",
            details = mapOf("path" to change.path),
        )
        is StructuralChange.TypeChanged -> VisualStructuralChange(
            type = "type_changed",
            description = "Type changed at ${change.path}: ${change.fromType} → ${change.toType}",
            details = mapOf(
                "path" to change.path,
                "fromType" to change.fromType,
                "toType" to change.toType,
            ),
        )
        is StructuralChange.ArraySizeChanged -> VisualStructuralChange(
            type = "array_size_changed",
            description = "Array size changed at ${change.path}: ${change.fromSize} → ${change.toSize}",
            details = mapOf(
                "path" to change.path,
                "fromSize" to change.fromSize.toString(),
                "toSize" to change.toSize.toString(),
            ),
        )
    }

    private fun formatJson(element: JsonElement): String {
        // Pretty print JSON for display
        return Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), element)
    }

    private fun formatValue(element: JsonElement): String = when (element) {
        is JsonPrimitive -> element.content
        is JsonObject -> "{${element.size} fields}"
        is JsonArray -> "[${element.size} items]"
    }

    private fun describePath(path: JsonPath): String = if (path.segments.isEmpty()) {
        "root"
    } else {
        path.segments.lastOrNull()?.let { segment ->
            when (segment) {
                is JsonPath.PathSegment.Field -> segment.name
                is JsonPath.PathSegment.Index -> "item ${segment.value}"
            }
        } ?: path.toString()
    }

    private fun findDifferences(str1: String, str2: String): List<TextHighlight> {
        // Simple character-level diff for highlighting
        val highlights = mutableListOf<TextHighlight>()
        val minLen = minOf(str1.length, str2.length)

        var start = -1
        for (i in 0 until minLen) {
            if (str1[i] != str2[i]) {
                if (start == -1) start = i
            } else if (start != -1) {
                highlights.add(TextHighlight(start, i))
                start = -1
            }
        }

        if (start != -1) {
            highlights.add(TextHighlight(start, minLen))
        }

        if (str1.length != str2.length) {
            highlights.add(TextHighlight(minLen, maxOf(str1.length, str2.length)))
        }

        return highlights
    }

    private fun groupIntoHunks(lines: List<UnifiedLine>): List<DiffHunk> {
        // Group lines into hunks for better visualization
        val hunks = mutableListOf<DiffHunk>()
        var currentHunk = mutableListOf<UnifiedLine>()
        var contextLines = 0

        lines.forEach { line ->
            when (line.type) {
                LineType.Unchanged -> {
                    contextLines++
                    if (contextLines > 3 && currentHunk.isNotEmpty()) {
                        // Start new hunk
                        hunks.add(DiffHunk(currentHunk))
                        currentHunk = mutableListOf()
                        contextLines = 0
                    }
                    currentHunk.add(line)
                }
                else -> {
                    contextLines = 0
                    currentHunk.add(line)
                }
            }
        }

        if (currentHunk.isNotEmpty()) {
            hunks.add(DiffHunk(currentHunk))
        }

        return hunks
    }
}
