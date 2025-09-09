package io.github.kamiazya.scopes.agentmanagement.domain.valueobject

/**
 * Represents the type of an agent in the system.
 * Agents can be either human users or AI assistants.
 */
enum class AgentType {
    /**
     * Human user agent
     */
    HUMAN,

    /**
     * AI assistant agent
     */
    AI,
}
