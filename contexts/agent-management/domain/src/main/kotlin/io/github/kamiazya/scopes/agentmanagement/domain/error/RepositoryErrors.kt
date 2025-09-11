package io.github.kamiazya.scopes.agentmanagement.domain.error

import io.github.kamiazya.scopes.agentmanagement.domain.service.SystemTimeProvider
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import kotlinx.datetime.Instant

/**
 * Error types for find operations.
 */
sealed class FindAgentError : AgentManagementError() {
    data class QueryTimeout(val operation: String, val timeoutMs: Long, override val occurredAt: Instant = SystemTimeProvider().now()) : FindAgentError()
    data class IndexCorruption(val agentId: AgentId, val message: String, override val occurredAt: Instant = SystemTimeProvider().now()) : FindAgentError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = SystemTimeProvider().now()) : FindAgentError()
}

/**
 * Error types for save operations.
 */
sealed class SaveAgentError : AgentManagementError() {
    data class ConcurrentModification(
        val agentId: AgentId,
        val expectedVersion: Int,
        val actualVersion: Int,
        override val occurredAt: Instant = SystemTimeProvider().now(),
    ) : SaveAgentError()
    data class ValidationFailed(val violations: List<String>, override val occurredAt: Instant = SystemTimeProvider().now()) : SaveAgentError()
    data class StorageQuotaExceeded(val currentSize: Long, val maxSize: Long, override val occurredAt: Instant = SystemTimeProvider().now()) : SaveAgentError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = SystemTimeProvider().now()) : SaveAgentError()
}

/**
 * Error types for delete operations.
 */
sealed class DeleteAgentError : AgentManagementError() {
    data class AgentInUse(val agentId: AgentId, val dependentEntities: List<String>, override val occurredAt: Instant = SystemTimeProvider().now()) :
        DeleteAgentError()
    data class ConcurrentModification(val agentId: AgentId, override val occurredAt: Instant = SystemTimeProvider().now()) : DeleteAgentError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = SystemTimeProvider().now()) : DeleteAgentError()
}

/**
 * Error types for exists operations.
 */
sealed class ExistsAgentError : AgentManagementError() {
    data class QueryTimeout(val operation: String, val timeoutMs: Long, override val occurredAt: Instant = SystemTimeProvider().now()) : ExistsAgentError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = SystemTimeProvider().now()) : ExistsAgentError()
}
