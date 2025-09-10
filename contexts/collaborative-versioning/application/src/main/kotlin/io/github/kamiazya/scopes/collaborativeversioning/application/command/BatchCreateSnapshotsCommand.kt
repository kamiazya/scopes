package io.github.kamiazya.scopes.collaborativeversioning.application.command

import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.BatchProcessingOptions
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceType
import kotlinx.datetime.Instant

/**
 * Command to create snapshots for multiple resources in batch.
 *
 * This command enables efficient batch processing of snapshots for
 * multiple resources, particularly useful for bulk operations.
 */
data class BatchCreateSnapshotsCommand(
    val resourceType: ResourceType,
    val authorId: AgentId,
    val message: String,
    val timestamp: Instant,
    val processingOptions: BatchProcessingOptions = BatchProcessingOptions(),
) {
    init {
        require(message.isNotBlank()) { "Batch snapshot message cannot be blank" }
    }
}
