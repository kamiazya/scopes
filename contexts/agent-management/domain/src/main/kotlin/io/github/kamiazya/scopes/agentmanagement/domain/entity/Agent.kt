package io.github.kamiazya.scopes.agentmanagement.domain.entity

import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentName
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentType
import kotlinx.datetime.Instant

/**
 * Core domain entity representing an Agent in the system.
 *
 * Agents can be either human users or AI assistants that interact
 * with the system and make changes to scopes.
 */
data class Agent(
    val id: AgentId,
    val name: AgentName,
    val type: AgentType,
    val capabilities: Set<String>,
    val metadata: Map<String, String>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    /**
     * Update the agent name with timestamp.
     */
    fun updateName(newName: AgentName, timestamp: Instant): Agent = copy(name = newName, updatedAt = timestamp)

    /**
     * Add a capability to the agent.
     */
    fun addCapability(capability: String, timestamp: Instant): Agent = copy(
        capabilities = capabilities + capability,
        updatedAt = timestamp,
    )

    /**
     * Remove a capability from the agent.
     */
    fun removeCapability(capability: String, timestamp: Instant): Agent = copy(
        capabilities = capabilities - capability,
        updatedAt = timestamp,
    )

    /**
     * Update agent metadata.
     */
    fun updateMetadata(key: String, value: String, timestamp: Instant): Agent = copy(
        metadata = metadata + (key to value),
        updatedAt = timestamp,
    )

    /**
     * Remove metadata key.
     */
    fun removeMetadata(key: String, timestamp: Instant): Agent = copy(
        metadata = metadata - key,
        updatedAt = timestamp,
    )

    /**
     * Check if agent has a specific capability.
     */
    fun hasCapability(capability: String): Boolean = capability in capabilities
}
