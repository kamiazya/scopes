package io.github.kamiazya.scopes.collaborativeversioning.domain.entity

import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ChangeType
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ChangesetId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionId
import kotlinx.datetime.Instant

/**
 * Core domain entity representing a Changeset in the system.
 *
 * A changeset captures a set of changes made to a resource by an agent.
 * It forms the basis of the versioning system, allowing for collaborative
 * editing and version control.
 */
data class Changeset(
    val id: ChangesetId,
    val resourceId: ResourceId,
    val authorId: AgentId,
    val changeType: ChangeType,
    val changes: List<Change>,
    val message: String,
    val parentVersionId: VersionId?,
    val createdAt: Instant,
    val appliedAt: Instant?,
) {
    /**
     * Check if this changeset has been applied.
     */
    fun isApplied(): Boolean = appliedAt != null

    /**
     * Mark this changeset as applied.
     */
    fun markAsApplied(timestamp: Instant): Changeset = copy(appliedAt = timestamp)

    /**
     * Add a change to this changeset.
     */
    fun addChange(change: Change): Changeset = copy(changes = changes + change)

    /**
     * Update the message of this changeset.
     */
    fun updateMessage(newMessage: String): Changeset = copy(message = newMessage)
}

/**
 * Represents a single change within a changeset.
 */
data class Change(
    val path: String,
    val operation: ChangeOperation,
    val previousValue: String?,
    val newValue: String?,
    val metadata: Map<String, String> = emptyMap(),
)

/**
 * Types of operations that can be performed on a resource.
 */
enum class ChangeOperation {
    ADD,
    MODIFY,
    DELETE,
    RENAME,
}
