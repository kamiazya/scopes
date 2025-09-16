package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.batch

import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceContent
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId

/**
 * Value object representing a request to create a snapshot for batch processing.
 */
data class SnapshotRequest(
    val resourceId: ResourceId,
    val content: ResourceContent,
    val authorId: AgentId,
    val message: String,
    val metadata: Map<String, String> = emptyMap(),
)
