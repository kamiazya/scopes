package io.github.kamiazya.scopes.agentmanagement.domain.error

import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentType

/**
 * Base error class for Agent Management bounded context.
 * All errors in this context should extend this class.
 */
sealed class AgentManagementError

/**
 * Errors related to Agent ID validation and operations.
 */
sealed class AgentIdError : AgentManagementError() {
    data class InvalidFormat(val providedValue: String, val expectedFormat: String) : AgentIdError()
}

/**
 * Errors related to Agent Name validation.
 */
sealed class AgentNameError : AgentManagementError() {
    data class EmptyName() : AgentNameError()
    data class NameTooShort(val minLength: Int, val actualLength: Int, val name: String) : AgentNameError()
    data class NameTooLong(val maxLength: Int, val actualLength: Int, val name: String) : AgentNameError()
    data class InvalidCharacters(val name: String, val invalidCharacters: Set<Char>) : AgentNameError()
}

/**
 * Errors related to Agent operations.
 */
sealed class AgentError : AgentManagementError() {
    data class AgentNotFound(val agentId: AgentId) : AgentError()

    data class DuplicateAgent(val agentId: AgentId, val agentName: String) : AgentError()

    data class InvalidAgentType(val providedType: String, val validTypes: Set<AgentType>) : AgentError()

    data class AgentAlreadyExists(val agentId: AgentId) : AgentError()
}

/**
 * Errors related to agent capability validation.
 */
sealed class AgentCapabilityError : AgentManagementError() {
    data class InvalidCapability(val capability: String, val reason: String) : AgentCapabilityError()

    data class DuplicateCapability(val agentId: AgentId, val capability: String) : AgentCapabilityError()

    data class CapabilityNotFound(val agentId: AgentId, val capability: String) : AgentCapabilityError()
}
