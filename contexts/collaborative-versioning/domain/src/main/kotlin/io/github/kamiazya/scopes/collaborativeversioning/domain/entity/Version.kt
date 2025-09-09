package io.github.kamiazya.scopes.collaborativeversioning.domain.entity

import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ChangesetId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionId
import kotlinx.datetime.Instant

/**
 * Core domain entity representing a Version of a resource.
 *
 * A version represents a specific state of a resource at a point in time,
 * created by applying one or more changesets.
 */
data class Version(
    val id: VersionId,
    val resourceId: ResourceId,
    val versionNumber: Int,
    val parentVersionId: VersionId?,
    val changesetIds: List<ChangesetId>,
    val authorId: AgentId,
    val message: String,
    val createdAt: Instant,
    val metadata: Map<String, String> = emptyMap(),
) {
    /**
     * Check if this is the first version (no parent).
     */
    fun isInitialVersion(): Boolean = parentVersionId == null

    /**
     * Add a changeset to this version.
     */
    fun addChangeset(changesetId: ChangesetId): Version = copy(changesetIds = changesetIds + changesetId)

    /**
     * Update version metadata.
     */
    fun updateMetadata(key: String, value: String): Version = copy(metadata = metadata + (key to value))

    /**
     * Remove metadata key.
     */
    fun removeMetadata(key: String): Version = copy(metadata = metadata - key)

    /**
     * Check if this version includes a specific changeset.
     */
    fun includesChangeset(changesetId: ChangesetId): Boolean = changesetId in changesetIds
}
