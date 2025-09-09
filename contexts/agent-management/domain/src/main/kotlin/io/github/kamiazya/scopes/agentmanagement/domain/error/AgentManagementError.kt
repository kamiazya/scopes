package io.github.kamiazya.scopes.agentmanagement.domain.error

import kotlinx.datetime.Instant

/**
 * Base error class for all agent management domain errors.
 */
sealed class AgentManagementError {
    abstract val occurredAt: Instant
}
