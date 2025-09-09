package io.github.kamiazya.scopes.agentmanagement.domain.valueobject

/**
 * Name of an agent.
 */
@JvmInline
value class AgentName(val value: String) {
    init {
        require(value.isNotBlank()) { "Agent name cannot be blank" }
        require(value.length <= 100) { "Agent name cannot exceed 100 characters" }
    }
}
