package io.github.kamiazya.scopes.collaborativeversioning.application.dto

import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.Snapshot
import kotlinx.datetime.Instant

/**
 * Data transfer object for Snapshot information.
 *
 * This DTO provides a simplified representation of snapshot data
 * for use in the application layer.
 */
data class SnapshotDto(
    val id: String,
    val resourceId: String,
    val versionId: String,
    val versionNumber: Int,
    val authorId: String,
    val message: String,
    val createdAt: Instant,
    val metadata: Map<String, String>,
    val contentSizeBytes: Int,
) {
    companion object {
        /**
         * Convert domain Snapshot to DTO.
         */
        fun fromDomain(snapshot: Snapshot): SnapshotDto = SnapshotDto(
            id = snapshot.id.toString(),
            resourceId = snapshot.resourceId.toString(),
            versionId = snapshot.versionId.toString(),
            versionNumber = snapshot.versionNumber.value,
            authorId = snapshot.authorId.toString(),
            message = snapshot.message,
            createdAt = snapshot.createdAt,
            metadata = snapshot.metadata,
            contentSizeBytes = snapshot.contentSizeInBytes(),
        )
    }
}
