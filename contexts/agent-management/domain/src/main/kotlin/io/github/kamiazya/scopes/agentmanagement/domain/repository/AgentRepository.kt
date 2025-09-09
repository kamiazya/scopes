package io.github.kamiazya.scopes.agentmanagement.domain.repository

import arrow.core.Either
import io.github.kamiazya.scopes.agentmanagement.domain.entity.Agent
import io.github.kamiazya.scopes.agentmanagement.domain.error.DeleteAgentError
import io.github.kamiazya.scopes.agentmanagement.domain.error.ExistsAgentError
import io.github.kamiazya.scopes.agentmanagement.domain.error.FindAgentError
import io.github.kamiazya.scopes.agentmanagement.domain.error.SaveAgentError
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentName
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Agent persistence operations.
 * Defined in domain layer following Dependency Inversion Principle.
 */
interface AgentRepository {

    /**
     * Find agent by its identifier.
     */
    suspend fun findById(id: AgentId): Either<FindAgentError, Agent?>

    /**
     * Find agent by name.
     */
    suspend fun findByName(name: AgentName): Either<FindAgentError, Agent?>

    /**
     * Find all agents by type.
     */
    suspend fun findByType(type: AgentType): Either<FindAgentError, Flow<Agent>>

    /**
     * Get all agents in the system.
     */
    suspend fun findAll(): Either<FindAgentError, Flow<Agent>>

    /**
     * Save an agent (create or update).
     */
    suspend fun save(agent: Agent): Either<SaveAgentError, Agent>

    /**
     * Delete an agent by ID.
     */
    suspend fun deleteById(id: AgentId): Either<DeleteAgentError, Unit>

    /**
     * Check if an agent exists.
     */
    suspend fun existsById(id: AgentId): Either<ExistsAgentError, Boolean>

    /**
     * Check if an agent exists by name.
     */
    suspend fun existsByName(name: AgentName): Either<ExistsAgentError, Boolean>
}
