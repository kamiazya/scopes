package io.github.kamiazya.scopes.collaborativeversioning.application.dto

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.MetadataChange
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.SizeChange
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.SnapshotDiff

/**
 * Data transfer object for snapshot diff information.
 */
data class SnapshotDiffDto(
    val fromSnapshotId: String,
    val toSnapshotId: String,
    val hasChanges: Boolean,
    val changeCount: Int,
    val addedPaths: Set<String>,
    val removedPaths: Set<String>,
    val modifiedPaths: Set<String>,
    val metadataChanges: Map<String, MetadataChangeDto>,
    val sizeChange: SizeChangeDto,
) {
    companion object {
        /**
         * Convert domain SnapshotDiff to DTO.
         */
        fun fromDomain(diff: SnapshotDiff): SnapshotDiffDto = SnapshotDiffDto(
            fromSnapshotId = diff.fromSnapshotId.toString(),
            toSnapshotId = diff.toSnapshotId.toString(),
            hasChanges = diff.hasChanges(),
            changeCount = diff.contentDiff.changeCount(),
            addedPaths = diff.contentDiff.addedPaths,
            removedPaths = diff.contentDiff.removedPaths,
            modifiedPaths = diff.contentDiff.modifiedPaths,
            metadataChanges = diff.metadataChanges.mapValues { (_, change) ->
                MetadataChangeDto.fromDomain(change)
            },
            sizeChange = SizeChangeDto.fromDomain(diff.sizeChange),
        )
    }
}

/**
 * DTO for metadata changes.
 */
sealed class MetadataChangeDto {
    data class Added(val value: String) : MetadataChangeDto()
    data class Removed(val oldValue: String) : MetadataChangeDto()
    data class Modified(val oldValue: String, val newValue: String) : MetadataChangeDto()

    companion object {
        fun fromDomain(change: MetadataChange): MetadataChangeDto = when (change) {
            is MetadataChange.Added -> Added(change.value)
            is MetadataChange.Removed -> Removed(change.oldValue)
            is MetadataChange.Modified -> Modified(change.oldValue, change.newValue)
        }
    }
}

/**
 * DTO for size change information.
 */
data class SizeChangeDto(val fromSize: Long, val toSize: Long, val difference: Long, val percentageChange: Double) {
    companion object {
        fun fromDomain(sizeChange: SizeChange): SizeChangeDto = SizeChangeDto(
            fromSize = sizeChange.fromSize,
            toSize = sizeChange.toSize,
            difference = sizeChange.difference,
            percentageChange = sizeChange.percentageChange,
        )
    }
}
