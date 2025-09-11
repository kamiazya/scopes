package io.github.kamiazya.scopes.agentmanagement.domain.error

import io.github.kamiazya.scopes.agentmanagement.domain.service.SystemTimeProvider
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentType
import kotlinx.datetime.Instant

/**
 * Base error class for Agent Management bounded context.
 * All errors in this context should extend this class.
 */
sealed class AgentManagementError {
    abstract val occurredAt: Instant
}

/**
 * Errors related to Agent ID validation and operations.
 */
sealed class AgentIdError : AgentManagementError() {
    data class InvalidFormat(val providedValue: String, val expectedFormat: String, override val occurredAt: Instant = SystemTimeProvider().now()) :
        AgentIdError()
}

/**
 * Errors related to Agent Name validation.
 */
sealed class AgentNameError : AgentManagementError() {
    data class EmptyName(override val occurredAt: Instant = SystemTimeProvider().now()) : AgentNameError()
    data class NameTooShort(val minLength: Int, val actualLength: Int, val name: String, override val occurredAt: Instant = SystemTimeProvider().now()) :
        AgentNameError()
    data class NameTooLong(val maxLength: Int, val actualLength: Int, val name: String, override val occurredAt: Instant = SystemTimeProvider().now()) :
        AgentNameError()
    data class InvalidCharacters(val name: String, val invalidCharacters: Set<Char>, override val occurredAt: Instant = SystemTimeProvider().now()) :
        AgentNameError()
}

/**
 * Errors related to Agent operations.
 */
sealed class AgentError : AgentManagementError() {
    data class AgentNotFound(val agentId: AgentId, override val occurredAt: Instant = SystemTimeProvider().now()) : AgentError()

    data class DuplicateAgent(val agentId: AgentId, val agentName: String, override val occurredAt: Instant = SystemTimeProvider().now()) : AgentError()

    data class InvalidAgentType(val providedType: String, val validTypes: Set<AgentType>, override val occurredAt: Instant = SystemTimeProvider().now()) :
        AgentError()

    data class AgentAlreadyExists(val agentId: AgentId, override val occurredAt: Instant = SystemTimeProvider().now()) : AgentError()
}

/**
 * Errors related to agent capability validation.
 */
sealed class AgentCapabilityError : AgentManagementError() {
    data class InvalidCapability(val capability: String, val reason: String, override val occurredAt: Instant = SystemTimeProvider().now()) :
        AgentCapabilityError()

    data class DuplicateCapability(val agentId: AgentId, val capability: String, override val occurredAt: Instant = SystemTimeProvider().now()) :
        AgentCapabilityError()

    data class CapabilityNotFound(val agentId: AgentId, val capability: String, override val occurredAt: Instant = SystemTimeProvider().now()) :
        AgentCapabilityError()
}
