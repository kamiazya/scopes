package io.github.kamiazya.scopes.collaborativeversioning.application.command

import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.SnapshotId
import kotlinx.datetime.Instant

/**
 * Command to restore a resource from a specific snapshot.
 *
 * This command triggers the restoration of a resource to a previous state
 * by creating a new snapshot with the content from the target snapshot.
 */
data class RestoreSnapshotCommand(
    val resourceId: ResourceId,
    val targetSnapshotId: SnapshotId,
    val authorId: AgentId,
    val message: String,
    val timestamp: Instant,
) {
    init {
        require(message.isNotBlank()) { "Restoration message cannot be blank" }
    }
}
