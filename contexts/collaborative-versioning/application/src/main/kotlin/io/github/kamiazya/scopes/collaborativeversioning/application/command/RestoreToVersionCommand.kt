package io.github.kamiazya.scopes.collaborativeversioning.application.command

import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionNumber
import kotlinx.datetime.Instant

/**
 * Command to restore a resource to a specific version number.
 *
 * This command triggers the restoration of a resource to a specific version
 * by creating a new snapshot with the content from that version.
 */
data class RestoreToVersionCommand(
    val resourceId: ResourceId,
    val targetVersion: VersionNumber,
    val authorId: AgentId,
    val message: String,
    val timestamp: Instant,
) {
    init {
        require(message.isNotBlank()) { "Restoration message cannot be blank" }
    }
}
