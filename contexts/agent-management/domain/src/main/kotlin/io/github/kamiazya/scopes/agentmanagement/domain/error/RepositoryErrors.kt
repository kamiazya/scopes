package io.github.kamiazya.scopes.agentmanagement.domain.error

import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId

/**
 * Error types for find operations.
 */
sealed class FindAgentError : AgentManagementError() {
    data class QueryTimeout(val operation: String, val timeoutMs: Long) : FindAgentError()
    data class IndexCorruption(val agentId: AgentId, val message: String) : FindAgentError()
    data class NetworkError(val message: String, val cause: Throwable?) : FindAgentError()
}

/**
 * Error types for save operations.
 */
sealed class SaveAgentError : AgentManagementError() {
    data class ConcurrentModification(val agentId: AgentId, val expectedVersion: Int, val actualVersion: Int) : SaveAgentError()
    data class ValidationFailed(val violations: List<String>) : SaveAgentError()
    data class StorageQuotaExceeded(val currentSize: Long, val maxSize: Long) : SaveAgentError()
    data class NetworkError(val message: String, val cause: Throwable?) : SaveAgentError()
}

/**
 * Error types for delete operations.
 */
sealed class DeleteAgentError : AgentManagementError() {
    data class AgentInUse(val agentId: AgentId, val dependentEntities: List<String>) : DeleteAgentError()
    data class ConcurrentModification(val agentId: AgentId) : DeleteAgentError()
    data class NetworkError(val message: String, val cause: Throwable?) : DeleteAgentError()
}

/**
 * Error types for exists operations.
 */
sealed class ExistsAgentError : AgentManagementError() {
    data class QueryTimeout(val operation: String, val timeoutMs: Long) : ExistsAgentError()
    data class NetworkError(val message: String, val cause: Throwable?) : ExistsAgentError()
}
