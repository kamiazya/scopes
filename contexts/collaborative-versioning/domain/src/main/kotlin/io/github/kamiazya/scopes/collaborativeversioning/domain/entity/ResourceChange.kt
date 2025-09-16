package io.github.kamiazya.scopes.collaborativeversioning.domain.entity

import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceChangeType
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionNumber
import kotlinx.datetime.Instant

/**
 * Core domain entity representing a change to a tracked resource.
 *
 * A ResourceChange captures metadata about a modification to a resource,
 * linking versions together in the history. It does not contain the actual
 * content changes - those are captured in snapshots.
 */
data class ResourceChange(
    val id: String, // Using simple string ID as this is internal to TrackedResource
    val resourceId: ResourceId,
    val fromVersionId: VersionId?,
    val toVersionId: VersionId,
    val fromVersionNumber: VersionNumber?,
    val toVersionNumber: VersionNumber,
    val authorId: AgentId,
    val changeType: ResourceChangeType,
    val message: String,
    val createdAt: Instant,
    val metadata: Map<String, String> = emptyMap(),
) {
    /**
     * Check if this is the initial change (no from version).
     */
    fun isInitialChange(): Boolean = fromVersionId == null

    /**
     * Get the version increment for this change.
     */
    fun versionIncrement(): Int = when {
        fromVersionNumber == null -> toVersionNumber.value
        else -> toVersionNumber.distanceFrom(fromVersionNumber)
    }

    /**
     * Check if this change represents a major version change.
     * A major change is one that increments the version by more than 1.
     */
    fun isMajorChange(): Boolean = versionIncrement() > 1
}
