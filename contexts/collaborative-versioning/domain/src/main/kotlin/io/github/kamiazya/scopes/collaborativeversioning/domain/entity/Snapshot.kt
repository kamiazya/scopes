package io.github.kamiazya.scopes.collaborativeversioning.domain.entity

import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceContent
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.SnapshotId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionNumber
import kotlinx.datetime.Instant

/**
 * Core domain entity representing a Snapshot of a resource.
 *
 * A snapshot captures the complete state of a resource at a specific version.
 * Snapshots are immutable - once created, they cannot be modified. This ensures
 * the integrity of the version history.
 */
data class Snapshot(
    val id: SnapshotId,
    val resourceId: ResourceId,
    val versionId: VersionId,
    val versionNumber: VersionNumber,
    val content: ResourceContent,
    val authorId: AgentId,
    val message: String,
    val createdAt: Instant,
    val metadata: Map<String, String> = emptyMap(),
) {
    /**
     * Check if this snapshot has specific metadata.
     */
    fun hasMetadata(key: String): Boolean = metadata.containsKey(key)

    /**
     * Get metadata value by key.
     */
    fun getMetadata(key: String): String? = metadata[key]

    /**
     * Check if this is the initial snapshot (version 1).
     */
    fun isInitialSnapshot(): Boolean = versionNumber.isInitial()

    /**
     * Get the size of the snapshot content in bytes.
     */
    fun contentSizeInBytes(): Int = content.sizeInBytes()
}
