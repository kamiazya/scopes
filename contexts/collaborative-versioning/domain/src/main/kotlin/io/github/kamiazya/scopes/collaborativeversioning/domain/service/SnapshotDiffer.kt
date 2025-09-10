package io.github.kamiazya.scopes.collaborativeversioning.domain.service

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.Snapshot
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SnapshotServiceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceContent
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.SnapshotId
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import kotlinx.serialization.json.*

/**
 * Service for calculating differences between snapshots.
 *
 * This service provides functionality to compare snapshots and identify
 * changes between versions, supporting both structural and content-level
 * comparisons.
 */
interface SnapshotDiffer {
    /**
     * Calculate the difference between two snapshots.
     *
     * @param fromSnapshot The starting snapshot
     * @param toSnapshot The target snapshot
     * @return Either an error or the calculated diff
     */
    fun calculateDiff(fromSnapshot: Snapshot, toSnapshot: Snapshot): Either<SnapshotServiceError, SnapshotDiff>

    /**
     * Calculate the difference between two resource contents.
     *
     * @param fromContent The starting content
     * @param toContent The target content
     * @return Either an error or the calculated content diff
     */
    fun calculateContentDiff(fromContent: ResourceContent, toContent: ResourceContent): Either<SnapshotServiceError, ContentDiff>

    /**
     * Check if two snapshots have identical content.
     *
     * @param snapshot1 The first snapshot
     * @param snapshot2 The second snapshot
     * @return true if contents are identical, false otherwise
     */
    fun areContentsIdentical(snapshot1: Snapshot, snapshot2: Snapshot): Boolean
}

/**
 * Represents the difference between two snapshots.
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

/**
 * Represents the difference between two resource contents.
 */
data class ContentDiff(val operations: List<DiffOperation>, val addedPaths: Set<String>, val removedPaths: Set<String>, val modifiedPaths: Set<String>) {
    /**
     * Check if there are any content changes.
     */
    fun hasChanges(): Boolean = operations.isNotEmpty()

    /**
     * Get the total number of changes.
     */
    fun changeCount(): Int = addedPaths.size + removedPaths.size + modifiedPaths.size
}

/**
 * Represents a single diff operation.
 */
sealed class DiffOperation {
    data class Add(val path: String, val value: JsonElement) : DiffOperation()
    data class Remove(val path: String, val oldValue: JsonElement) : DiffOperation()
    data class Replace(val path: String, val oldValue: JsonElement, val newValue: JsonElement) : DiffOperation()
    data class Move(val fromPath: String, val toPath: String, val value: JsonElement) : DiffOperation()
}

/**
 * Represents a change in metadata.
 */
sealed class MetadataChange {
    data class Added(val value: String) : MetadataChange()
    data class Removed(val oldValue: String) : MetadataChange()
    data class Modified(val oldValue: String, val newValue: String) : MetadataChange()
}

/**
 * Represents a change in size.
 */
data class SizeChange(val fromSize: Long, val toSize: Long) {
    val difference: Long = toSize - fromSize
    val percentageChange: Double = if (fromSize > 0) {
        (difference.toDouble() / fromSize) * 100
    } else {
        100.0
    }
}

/**
 * Default implementation of SnapshotDiffer using JSON diff algorithms.
 */
class DefaultSnapshotDiffer(private val logger: Logger = ConsoleLogger("SnapshotDiffer")) : SnapshotDiffer {

    override fun calculateDiff(fromSnapshot: Snapshot, toSnapshot: Snapshot): Either<SnapshotServiceError, SnapshotDiff> = either {
        logger.debug(
            "Calculating diff between snapshots",
            mapOf(
                "fromSnapshotId" to fromSnapshot.id.toString(),
                "toSnapshotId" to toSnapshot.id.toString(),
                "fromVersion" to fromSnapshot.versionNumber.toString(),
                "toVersion" to toSnapshot.versionNumber.toString(),
            ),
        )

        // Calculate content diff
        val contentDiff = calculateContentDiff(
            fromContent = fromSnapshot.content,
            toContent = toSnapshot.content,
        ).bind()

        // Calculate metadata changes
        val metadataChanges = calculateMetadataChanges(
            fromMetadata = fromSnapshot.metadata,
            toMetadata = toSnapshot.metadata,
        )

        // Calculate size change
        val sizeChange = SizeChange(
            fromSize = fromSnapshot.contentSizeInBytes().toLong(),
            toSize = toSnapshot.contentSizeInBytes().toLong(),
        )

        logger.debug(
            "Diff calculation completed",
            mapOf(
                "contentChanges" to contentDiff.changeCount(),
                "metadataChanges" to metadataChanges.size,
                "sizeChange" to sizeChange.difference,
            ),
        )

        SnapshotDiff(
            fromSnapshotId = fromSnapshot.id,
            toSnapshotId = toSnapshot.id,
            contentDiff = contentDiff,
            metadataChanges = metadataChanges,
            sizeChange = sizeChange,
        )
    }

    override fun calculateContentDiff(fromContent: ResourceContent, toContent: ResourceContent): Either<SnapshotServiceError, ContentDiff> = either {
        val fromJson = fromContent.value
        val toJson = toContent.value

        val operations = mutableListOf<DiffOperation>()
        val addedPaths = mutableSetOf<String>()
        val removedPaths = mutableSetOf<String>()
        val modifiedPaths = mutableSetOf<String>()

        // Calculate diff recursively
        calculateJsonDiff(
            path = "",
            fromJson = fromJson,
            toJson = toJson,
            operations = operations,
            addedPaths = addedPaths,
            removedPaths = removedPaths,
            modifiedPaths = modifiedPaths,
        )

        ContentDiff(
            operations = operations,
            addedPaths = addedPaths,
            removedPaths = removedPaths,
            modifiedPaths = modifiedPaths,
        )
    }

    override fun areContentsIdentical(snapshot1: Snapshot, snapshot2: Snapshot): Boolean = snapshot1.content == snapshot2.content

    private fun calculateJsonDiff(
        path: String,
        fromJson: JsonElement?,
        toJson: JsonElement?,
        operations: MutableList<DiffOperation>,
        addedPaths: MutableSet<String>,
        removedPaths: MutableSet<String>,
        modifiedPaths: MutableSet<String>,
    ) {
        when {
            fromJson == null && toJson != null -> {
                operations.add(DiffOperation.Add(path, toJson))
                addedPaths.add(path)
            }
            fromJson != null && toJson == null -> {
                operations.add(DiffOperation.Remove(path, fromJson))
                removedPaths.add(path)
            }
            fromJson != null && toJson != null && fromJson != toJson -> {
                when {
                    fromJson is JsonObject && toJson is JsonObject -> {
                        calculateObjectDiff(path, fromJson, toJson, operations, addedPaths, removedPaths, modifiedPaths)
                    }
                    fromJson is JsonArray && toJson is JsonArray -> {
                        calculateArrayDiff(path, fromJson, toJson, operations, addedPaths, removedPaths, modifiedPaths)
                    }
                    else -> {
                        operations.add(DiffOperation.Replace(path, fromJson, toJson))
                        modifiedPaths.add(path)
                    }
                }
            }
        }
    }

    private fun calculateObjectDiff(
        path: String,
        fromObject: JsonObject,
        toObject: JsonObject,
        operations: MutableList<DiffOperation>,
        addedPaths: MutableSet<String>,
        removedPaths: MutableSet<String>,
        modifiedPaths: MutableSet<String>,
    ) {
        val allKeys = (fromObject.keys + toObject.keys).toSet()

        for (key in allKeys) {
            val childPath = if (path.isEmpty()) key else "$path.$key"
            val fromValue = fromObject[key]
            val toValue = toObject[key]

            calculateJsonDiff(
                path = childPath,
                fromJson = fromValue,
                toJson = toValue,
                operations = operations,
                addedPaths = addedPaths,
                removedPaths = removedPaths,
                modifiedPaths = modifiedPaths,
            )
        }
    }

    private fun calculateArrayDiff(
        path: String,
        fromArray: JsonArray,
        toArray: JsonArray,
        operations: MutableList<DiffOperation>,
        addedPaths: MutableSet<String>,
        removedPaths: MutableSet<String>,
        modifiedPaths: MutableSet<String>,
    ) {
        // Simple array diff - could be enhanced with more sophisticated algorithms
        val maxSize = maxOf(fromArray.size, toArray.size)

        for (i in 0 until maxSize) {
            val indexPath = "$path[$i]"
            val fromValue = fromArray.getOrNull(i)
            val toValue = toArray.getOrNull(i)

            calculateJsonDiff(
                path = indexPath,
                fromJson = fromValue,
                toJson = toValue,
                operations = operations,
                addedPaths = addedPaths,
                removedPaths = removedPaths,
                modifiedPaths = modifiedPaths,
            )
        }
    }

    private fun calculateMetadataChanges(fromMetadata: Map<String, String>, toMetadata: Map<String, String>): Map<String, MetadataChange> {
        val changes = mutableMapOf<String, MetadataChange>()
        val allKeys = (fromMetadata.keys + toMetadata.keys).toSet()

        for (key in allKeys) {
            when {
                key !in fromMetadata -> {
                    changes[key] = MetadataChange.Added(toMetadata[key]!!)
                }
                key !in toMetadata -> {
                    changes[key] = MetadataChange.Removed(fromMetadata[key]!!)
                }
                fromMetadata[key] != toMetadata[key] -> {
                    changes[key] = MetadataChange.Modified(
                        oldValue = fromMetadata[key]!!,
                        newValue = toMetadata[key]!!,
                    )
                }
            }
        }

        return changes
    }
}
