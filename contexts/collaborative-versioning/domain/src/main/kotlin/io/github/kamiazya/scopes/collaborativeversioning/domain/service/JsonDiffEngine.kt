package io.github.kamiazya.scopes.collaborativeversioning.domain.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.JsonDiffError
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ChangeSet
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.JsonChange
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.JsonDiff
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.JsonPath
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.StructuralChange
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import kotlinx.serialization.json.*

/**
 * Domain service for calculating comprehensive JSON differences.
 *
 * This service provides advanced diff calculation capabilities including:
 * - Deep structural comparison
 * - Array diff with move detection
 * - Path-based change tracking
 * - Semantic diff support
 * - Performance optimizations for large documents
 */
interface JsonDiffEngine {
    /**
     * Calculate comprehensive diff between two JSON documents.
     *
     * @param source The source JSON document
     * @param target The target JSON document
     * @return Either an error or the calculated JsonDiff
     */
    suspend fun calculateDiff(source: JsonElement, target: JsonElement): Either<JsonDiffError, JsonDiff>

    /**
     * Calculate diff and generate a change set.
     *
     * @param source The source JSON document
     * @param target The target JSON document
     * @param metadata Optional metadata for the change set
     * @return Either an error or the generated ChangeSet
     */
    suspend fun generateChangeSet(source: JsonElement, target: JsonElement, metadata: Map<String, String> = emptyMap()): Either<JsonDiffError, ChangeSet>

    /**
     * Check if two JSON documents are semantically equivalent.
     *
     * @param json1 First JSON document
     * @param json2 Second JSON document
     * @return true if semantically equivalent, false otherwise
     */
    fun areEquivalent(json1: JsonElement, json2: JsonElement): Boolean

    /**
     * Calculate the size of changes in bytes.
     *
     * @param diff The diff to measure
     * @return Size of changes in bytes
     */
    fun calculateDiffSize(diff: JsonDiff): Long
}

/**
 * Default implementation of JsonDiffEngine with advanced diff algorithms.
 */
class DefaultJsonDiffEngine(private val logger: Logger = ConsoleLogger("JsonDiffEngine"), private val config: DiffEngineConfig = DiffEngineConfig.default()) :
    JsonDiffEngine {

    override suspend fun calculateDiff(source: JsonElement, target: JsonElement): Either<JsonDiffError, JsonDiff> = either {
        logger.debug(
            "Starting diff calculation",
            mapOf(
                "sourceType" to source::class.simpleName,
                "targetType" to target::class.simpleName,
                "sourceSize" to estimateSize(source),
                "targetSize" to estimateSize(target),
            ) as Map<String, Any>,
        )

        // Validate input size constraints
        ensure(estimateSize(source) <= config.maxDocumentSize) {
            JsonDiffError.DocumentTooLarge(
                actualSize = estimateSize(source),
                maxSize = config.maxDocumentSize,
            )
        }
        ensure(estimateSize(target) <= config.maxDocumentSize) {
            JsonDiffError.DocumentTooLarge(
                actualSize = estimateSize(target),
                maxSize = config.maxDocumentSize,
            )
        }

        val changes = mutableListOf<JsonChange>()
        val structuralChanges = mutableListOf<StructuralChange>()
        val context = DiffContext(
            changes = changes,
            structuralChanges = structuralChanges,
            config = config,
        )

        // Calculate diff recursively
        calculateJsonDiff(
            path = JsonPath.root(),
            source = source,
            target = target,
            context = context,
        )

        // Post-process for optimization
        val optimizedChanges = if (config.optimizeChanges) {
            optimizeChanges(changes)
        } else {
            changes
        }

        logger.debug(
            "Diff calculation completed",
            mapOf(
                "totalChanges" to optimizedChanges.size,
                "structuralChanges" to structuralChanges.size,
                "optimizationApplied" to config.optimizeChanges,
            ),
        )

        JsonDiff(
            changes = optimizedChanges,
            structuralChanges = structuralChanges,
            metadata = mapOf(
                "engineVersion" to "1.0.0",
                "calculatedAt" to kotlinx.datetime.Clock.System.now().toString(),
            ),
        )
    }

    override suspend fun generateChangeSet(source: JsonElement, target: JsonElement, metadata: Map<String, String>): Either<JsonDiffError, ChangeSet> = either {
        val diff = calculateDiff(source, target).bind()

        ChangeSet.create(
            diff = diff,
            source = source,
            target = target,
            metadata = metadata + mapOf(
                "generatedBy" to "DefaultJsonDiffEngine",
                "generatedAt" to kotlinx.datetime.Clock.System.now().toString(),
            ),
        ).mapLeft { error ->
            JsonDiffError.ChangeSetGenerationFailed(
                reason = error.toString(),
            )
        }.bind()
    }

    override fun areEquivalent(json1: JsonElement, json2: JsonElement): Boolean = normalizeJson(json1) == normalizeJson(json2)

    override fun calculateDiffSize(diff: JsonDiff): Long {
        // Calculate the size of changes in bytes
        var totalSize = 0L

        diff.changes.forEach { change ->
            totalSize += when (change) {
                is JsonChange.Add -> change.value.toString().length
                is JsonChange.Remove -> change.oldValue.toString().length
                is JsonChange.Replace -> {
                    change.oldValue.toString().length + change.newValue.toString().length
                }
                is JsonChange.Move -> {
                    // Move operations have minimal size impact
                    change.from.toString().length + change.to.toString().length
                }
            }
        }

        return totalSize
    }

    private fun calculateJsonDiff(path: JsonPath, source: JsonElement?, target: JsonElement?, context: DiffContext) {
        when {
            source == null && target != null -> {
                context.addChange(JsonChange.Add(path, target))
                context.addStructuralChange(
                    StructuralChange.FieldAdded(path.toString()),
                )
            }
            source != null && target == null -> {
                context.addChange(JsonChange.Remove(path, source))
                context.addStructuralChange(
                    StructuralChange.FieldRemoved(path.toString()),
                )
            }
            source != null && target != null -> {
                when {
                    source == target -> {
                        // No change
                    }
                    source is JsonObject && target is JsonObject -> {
                        calculateObjectDiff(path, source, target, context)
                    }
                    source is JsonArray && target is JsonArray -> {
                        calculateArrayDiff(path, source, target, context)
                    }
                    else -> {
                        // Type change or value change
                        context.addChange(JsonChange.Replace(path, source, target))
                        if (source::class != target::class) {
                            context.addStructuralChange(
                                StructuralChange.TypeChanged(
                                    path = path.toString(),
                                    fromType = getJsonType(source),
                                    toType = getJsonType(target),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun calculateObjectDiff(path: JsonPath, source: JsonObject, target: JsonObject, context: DiffContext) {
        val allKeys = (source.keys + target.keys).toSet()

        allKeys.forEach { key ->
            val childPath = path.append(key)
            val sourceValue = source[key]
            val targetValue = target[key]

            calculateJsonDiff(
                path = childPath,
                source = sourceValue,
                target = targetValue,
                context = context,
            )
        }
    }

    private fun calculateArrayDiff(path: JsonPath, source: JsonArray, target: JsonArray, context: DiffContext) {
        if (config.detectArrayMoves && source.isNotEmpty() && target.isNotEmpty()) {
            // Use LCS-based diff for better move detection
            calculateLcsArrayDiff(path, source, target, context)
        } else {
            // Simple index-based diff
            calculateSimpleArrayDiff(path, source, target, context)
        }

        // Record structural change if array size changed
        if (source.size != target.size) {
            context.addStructuralChange(
                StructuralChange.ArraySizeChanged(
                    path = path.toString(),
                    fromSize = source.size,
                    toSize = target.size,
                ),
            )
        }
    }

    private fun calculateSimpleArrayDiff(path: JsonPath, source: JsonArray, target: JsonArray, context: DiffContext) {
        val maxIndex = maxOf(source.size, target.size)

        for (i in 0 until maxIndex) {
            val indexPath = path.appendIndex(i)
            val sourceValue = source.getOrNull(i)
            val targetValue = target.getOrNull(i)

            calculateJsonDiff(
                path = indexPath,
                source = sourceValue,
                target = targetValue,
                context = context,
            )
        }
    }

    private fun calculateLcsArrayDiff(path: JsonPath, source: JsonArray, target: JsonArray, context: DiffContext) {
        // Implement LCS-based diff algorithm for better move detection
        val sourceItems = source.mapIndexed { index, item -> IndexedItem(index, item) }
        val targetItems = target.mapIndexed { index, item -> IndexedItem(index, item) }

        val lcs = findLcs(sourceItems, targetItems)
        val moves = detectMoves(sourceItems, targetItems, lcs)

        // Apply moves first
        moves.forEach { move ->
            context.addChange(
                JsonChange.Move(
                    from = path.appendIndex(move.fromIndex),
                    to = path.appendIndex(move.toIndex),
                    value = move.value,
                ),
            )
        }

        // Then handle additions, removals, and modifications
        val movedSourceIndices = moves.map { it.fromIndex }.toSet()
        val movedTargetIndices = moves.map { it.toIndex }.toSet()

        // Process remaining changes
        source.forEachIndexed { index, item ->
            if (index !in movedSourceIndices && !lcs.any { it.sourceIndex == index }) {
                context.addChange(
                    JsonChange.Remove(
                        path = path.appendIndex(index),
                        oldValue = item,
                    ),
                )
            }
        }

        target.forEachIndexed { index, item ->
            if (index !in movedTargetIndices && !lcs.any { it.targetIndex == index }) {
                context.addChange(
                    JsonChange.Add(
                        path = path.appendIndex(index),
                        value = item,
                    ),
                )
            }
        }
    }

    private fun findLcs(source: List<IndexedItem>, target: List<IndexedItem>): List<LcsMatch> {
        // Simplified LCS implementation
        val matches = mutableListOf<LcsMatch>()

        source.forEach { sourceItem ->
            target.find { targetItem ->
                targetItem.value == sourceItem.value &&
                    matches.none { it.targetIndex == targetItem.index }
            }?.let { targetItem ->
                matches.add(
                    LcsMatch(
                        sourceIndex = sourceItem.index,
                        targetIndex = targetItem.index,
                        value = sourceItem.value,
                    ),
                )
            }
        }

        return matches.sortedBy { it.sourceIndex }
    }

    private fun detectMoves(source: List<IndexedItem>, target: List<IndexedItem>, lcs: List<LcsMatch>): List<MoveOperation> {
        val moves = mutableListOf<MoveOperation>()

        lcs.forEach { match ->
            if (match.sourceIndex != match.targetIndex) {
                moves.add(
                    MoveOperation(
                        fromIndex = match.sourceIndex,
                        toIndex = match.targetIndex,
                        value = match.value,
                    ),
                )
            }
        }

        return moves
    }

    private fun optimizeChanges(changes: List<JsonChange>): List<JsonChange> {
        // Group adjacent changes and optimize
        val optimized = mutableListOf<JsonChange>()
        var i = 0

        while (i < changes.size) {
            val change = changes[i]

            // Look for optimization opportunities
            when (change) {
                is JsonChange.Remove -> {
                    // Check if followed by an Add at the same path (convert to Replace)
                    val nextChange = changes.getOrNull(i + 1)
                    if (nextChange is JsonChange.Add && nextChange.path == change.path) {
                        optimized.add(
                            JsonChange.Replace(
                                path = change.path,
                                oldValue = change.oldValue,
                                newValue = nextChange.value,
                            ),
                        )
                        i += 2
                    } else {
                        optimized.add(change)
                        i++
                    }
                }
                else -> {
                    optimized.add(change)
                    i++
                }
            }
        }

        return optimized
    }

    private fun normalizeJson(json: JsonElement): JsonElement = when (json) {
        is JsonObject -> {
            // Sort keys for consistent comparison
            val sorted = json.entries.sortedBy { it.key }
                .associate { (k, v) -> k to normalizeJson(v) }
            JsonObject(sorted)
        }
        is JsonArray -> {
            JsonArray(json.map { normalizeJson(it) })
        }
        else -> json
    }

    private fun estimateSize(json: JsonElement): Long = json.toString().length.toLong()

    private fun getJsonType(json: JsonElement): String = when (json) {
        is JsonObject -> "object"
        is JsonArray -> "array"
        is JsonPrimitive -> when {
            json.isString -> "string"
            json.booleanOrNull != null -> "boolean"
            json.longOrNull != null -> "number"
            json.doubleOrNull != null -> "number"
            else -> "null"
        }
    }

    private data class DiffContext(val changes: MutableList<JsonChange>, val structuralChanges: MutableList<StructuralChange>, val config: DiffEngineConfig) {
        fun addChange(change: JsonChange) {
            changes.add(change)
        }

        fun addStructuralChange(change: StructuralChange) {
            structuralChanges.add(change)
        }
    }

    private data class IndexedItem(val index: Int, val value: JsonElement)

    private data class LcsMatch(val sourceIndex: Int, val targetIndex: Int, val value: JsonElement)

    private data class MoveOperation(val fromIndex: Int, val toIndex: Int, val value: JsonElement)
}

/**
 * Configuration for the diff engine.
 */
data class DiffEngineConfig(
    val maxDocumentSize: Long = 10 * 1024 * 1024, // 10MB
    val detectArrayMoves: Boolean = true,
    val optimizeChanges: Boolean = true,
    val maxDiffDepth: Int = 100,
) {
    companion object {
        fun default() = DiffEngineConfig()

        fun performance() = DiffEngineConfig(
            detectArrayMoves = false,
            optimizeChanges = false,
        )

        fun quality() = DiffEngineConfig(
            detectArrayMoves = true,
            optimizeChanges = true,
            maxDiffDepth = 200,
        )
    }
}
