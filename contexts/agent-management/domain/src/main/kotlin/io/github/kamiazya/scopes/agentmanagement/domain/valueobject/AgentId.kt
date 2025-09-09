package io.github.kamiazya.scopes.agentmanagement.domain.valueobject

import io.github.kamiazya.scopes.platform.commons.id.ULID

/**
 * Unique identifier for an agent.
 * An agent can be a human user or an AI assistant.
 */
@JvmInline
value class AgentId(val value: String) {
    companion object {
        fun generate(): AgentId = AgentId(ULID.generate().toString())
    }
}
