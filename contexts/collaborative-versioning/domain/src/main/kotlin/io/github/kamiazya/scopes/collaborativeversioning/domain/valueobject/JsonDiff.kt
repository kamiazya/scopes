package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.JsonDiffError
import kotlinx.serialization.json.JsonElement

/**
 * Represents a comprehensive JSON diff between two documents.
 */
data class JsonDiff(val changes: List<JsonChange>, val structuralChanges: List<StructuralChange>, val metadata: Map<String, String> = emptyMap()) {
    /**
     * Get total number of changes.
     */
    fun changeCount(): Int = changes.size

    /**
     * Check if there are any changes.
     */
    fun hasChanges(): Boolean = changes.isNotEmpty()

    /**
     * Check if there are structural changes.
     */
    fun hasStructuralChanges(): Boolean = structuralChanges.isNotEmpty()

    /**
     * Get changes by type.
     */
    inline fun <reified T : JsonChange> changesByType(): List<T> = changes.filterIsInstance<T>()

    /**
     * Get all affected paths.
     */
    fun affectedPaths(): Set<JsonPath> = changes.flatMap { change ->
        when (change) {
            is JsonChange.Add -> setOf(change.path)
            is JsonChange.Remove -> setOf(change.path)
            is JsonChange.Replace -> setOf(change.path)
            is JsonChange.Move -> setOf(change.from, change.to)
        }
    }.toSet()

    /**
     * Check if a specific path is affected.
     */
    fun isPathAffected(path: JsonPath): Boolean = affectedPaths().contains(path)

    /**
     * Get changes for a specific path.
     */
    fun changesForPath(path: JsonPath): List<JsonChange> = changes.filter { change ->
        when (change) {
            is JsonChange.Add -> change.path == path
            is JsonChange.Remove -> change.path == path
            is JsonChange.Replace -> change.path == path
            is JsonChange.Move -> change.from == path || change.to == path
        }
    }

    /**
     * Apply this diff to a JSON document.
     */
    fun apply(source: JsonElement): Either<JsonDiffError, JsonElement> = either {
        var result = source

        changes.forEach { change ->
            result = when (change) {
                is JsonChange.Add -> applyAdd(result, change).bind()
                is JsonChange.Remove -> applyRemove(result, change).bind()
                is JsonChange.Replace -> applyReplace(result, change).bind()
                is JsonChange.Move -> applyMove(result, change).bind()
            }
        }

        result
    }

    private fun applyAdd(document: JsonElement, change: JsonChange.Add): Either<JsonDiffError, JsonElement> = either {
        // Implementation would modify the document at the specified path
        // This is a simplified version - full implementation would handle nested paths
        document
    }

    private fun applyRemove(document: JsonElement, change: JsonChange.Remove): Either<JsonDiffError, JsonElement> = either {
        // Implementation would remove the element at the specified path
        document
    }

    private fun applyReplace(document: JsonElement, change: JsonChange.Replace): Either<JsonDiffError, JsonElement> = either {
        // Implementation would replace the element at the specified path
        document
    }

    private fun applyMove(document: JsonElement, change: JsonChange.Move): Either<JsonDiffError, JsonElement> = either {
        // Implementation would move the element from one path to another
        document
    }

    companion object {
        /**
         * Create an empty diff.
         */
        fun empty() = JsonDiff(
            changes = emptyList(),
            structuralChanges = emptyList(),
        )

        /**
         * Merge multiple diffs into one.
         */
        fun merge(diffs: List<JsonDiff>): JsonDiff = JsonDiff(
            changes = diffs.flatMap { it.changes },
            structuralChanges = diffs.flatMap { it.structuralChanges }.distinct(),
            metadata = diffs.flatMap { it.metadata.entries }.associate { it.toPair() },
        )
    }
}

/**
 * Represents a JSON path in a document.
 */
data class JsonPath(val segments: List<PathSegment>) {
    override fun toString(): String = if (segments.isEmpty()) {
        "$"
    } else {
        "$" + segments.joinToString("") { segment ->
            when (segment) {
                is PathSegment.Field -> ".${segment.name}"
                is PathSegment.Index -> "[${segment.value}]"
            }
        }
    }

    fun append(field: String): JsonPath = JsonPath(segments + PathSegment.Field(field))

    fun appendIndex(index: Int): JsonPath = JsonPath(segments + PathSegment.Index(index))

    fun parent(): JsonPath? = if (segments.isEmpty()) null else JsonPath(segments.dropLast(1))

    fun isAncestorOf(other: JsonPath): Boolean = other.segments.size > segments.size &&
        other.segments.take(segments.size) == segments

    fun isDescendantOf(other: JsonPath): Boolean = other.isAncestorOf(this)

    companion object {
        fun root(): JsonPath = JsonPath(emptyList())

        fun parse(path: String): Either<JsonDiffError, JsonPath> = either {
            ensure(path.startsWith("$")) {
                JsonDiffError.InvalidJsonStructure(
                    reason = "Path must start with $",
                    path = path,
                )
            }

            // Simple parser - full implementation would be more robust
            val segments = mutableListOf<PathSegment>()
            var remaining = path.substring(1)

            while (remaining.isNotEmpty()) {
                when {
                    remaining.startsWith(".") -> {
                        val fieldEnd = remaining.indexOfAny(charArrayOf('.', '['), 1)
                            .takeIf { it >= 0 } ?: remaining.length
                        val fieldName = remaining.substring(1, fieldEnd)
                        segments.add(PathSegment.Field(fieldName))
                        remaining = remaining.substring(fieldEnd)
                    }
                    remaining.startsWith("[") -> {
                        val indexEnd = remaining.indexOf(']')
                        ensure(indexEnd >= 0) {
                            JsonDiffError.InvalidJsonStructure(
                                reason = "Unclosed array index",
                                path = path,
                            )
                        }
                        val indexStr = remaining.substring(1, indexEnd)
                        val index = indexStr.toIntOrNull()
                        ensureNotNull(index) {
                            JsonDiffError.InvalidJsonStructure(
                                reason = "Invalid array index: $indexStr",
                                path = path,
                            )
                        }
                        segments.add(PathSegment.Index(index))
                        remaining = remaining.substring(indexEnd + 1)
                    }
                    else -> {
                        raise(
                            JsonDiffError.InvalidJsonStructure(
                                reason = "Unexpected character at position ${path.length - remaining.length}",
                                path = path,
                            ),
                        )
                    }
                }
            }

            JsonPath(segments)
        }
    }

    sealed class PathSegment {
        data class Field(val name: String) : PathSegment()

        data class Index(val value: Int) : PathSegment()
    }
}

/**
 * Represents a change in a JSON document.
 */
sealed class JsonChange {
    data class Add(val path: JsonPath, val value: JsonElement) : JsonChange()

    data class Remove(val path: JsonPath, val oldValue: JsonElement) : JsonChange()

    data class Replace(val path: JsonPath, val oldValue: JsonElement, val newValue: JsonElement) : JsonChange()

    data class Move(val from: JsonPath, val to: JsonPath, val value: JsonElement) : JsonChange()
}

/**
 * Represents structural changes in the document.
 */
sealed class StructuralChange {
    data class FieldAdded(val path: String) : StructuralChange()

    data class FieldRemoved(val path: String) : StructuralChange()

    data class TypeChanged(val path: String, val fromType: String, val toType: String) : StructuralChange()

    data class ArraySizeChanged(val path: String, val fromSize: Int, val toSize: Int) : StructuralChange()
}
