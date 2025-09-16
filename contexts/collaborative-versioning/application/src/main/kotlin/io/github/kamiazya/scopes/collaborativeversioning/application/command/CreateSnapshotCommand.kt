package io.github.kamiazya.scopes.collaborativeversioning.application.command

import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceContent
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import kotlinx.datetime.Instant

/**
 * Command to create a new snapshot for a tracked resource.
 *
 * This command captures all the necessary information to create a snapshot
 * of a resource's current state, including metadata and authorship details.
 */
data class CreateSnapshotCommand(
    val resourceId: ResourceId,
    val content: ResourceContent,
    val authorId: AgentId,
    val message: String,
    val metadata: Map<String, String> = emptyMap(),
    val timestamp: Instant,
) {
    init {
        require(message.isNotBlank()) { "Snapshot message cannot be blank" }
        require(metadata.size <= MAX_METADATA_ENTRIES) {
            "Too many metadata entries: ${metadata.size}, maximum is $MAX_METADATA_ENTRIES"
        }
    }

    companion object {
        const val MAX_METADATA_ENTRIES = 50
    }
}
