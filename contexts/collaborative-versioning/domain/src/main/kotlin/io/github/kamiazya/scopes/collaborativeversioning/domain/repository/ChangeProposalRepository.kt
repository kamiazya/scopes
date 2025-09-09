package io.github.kamiazya.scopes.collaborativeversioning.domain.repository

import arrow.core.Either
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.*
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.ChangeProposal
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for ChangeProposal persistence operations.
 *
 * This repository manages the storage and retrieval of change proposals,
 * including their complete history of changes and review comments.
 * Each operation returns specific error types to provide detailed failure information.
 */
interface ChangeProposalRepository {

    /**
     * Find a change proposal by its identifier.
     *
     * @param id The proposal identifier
     * @return Either an error or the change proposal (null if not found)
     */
    suspend fun findById(id: ProposalId): Either<FindChangeProposalError, ChangeProposal?>

    /**
     * Find all change proposals for a specific resource.
     *
     * @param resourceId The resource identifier
     * @return Either an error or a flow of change proposals
     */
    suspend fun findByResourceId(resourceId: ResourceId): Either<FindChangeProposalError, Flow<ChangeProposal>>

    /**
     * Find all change proposals created by a specific author.
     *
     * @param author The author of the proposals
     * @return Either an error or a flow of change proposals
     */
    suspend fun findByAuthor(author: Author): Either<FindChangeProposalError, Flow<ChangeProposal>>

    /**
     * Find all change proposals in a specific state.
     *
     * @param state The proposal state
     * @return Either an error or a flow of change proposals
     */
    suspend fun findByState(state: ProposalState): Either<FindChangeProposalError, Flow<ChangeProposal>>

    /**
     * Find all change proposals for a resource in specific states.
     *
     * @param resourceId The resource identifier
     * @param states The list of states to filter by
     * @return Either an error or a flow of change proposals
     */
    suspend fun findByResourceIdAndStates(resourceId: ResourceId, states: List<ProposalState>): Either<FindChangeProposalError, Flow<ChangeProposal>>

    /**
     * Get all change proposals in the system.
     *
     * @return Either an error or a flow of all change proposals
     */
    suspend fun findAll(): Either<FindChangeProposalError, Flow<ChangeProposal>>

    /**
     * Save a change proposal (create or update).
     *
     * @param proposal The change proposal to save
     * @return Either an error or the saved proposal
     */
    suspend fun save(proposal: ChangeProposal): Either<SaveChangeProposalError, ChangeProposal>

    /**
     * Delete a change proposal by ID.
     *
     * @param id The proposal identifier
     * @return Either an error or Unit on success
     */
    suspend fun deleteById(id: ProposalId): Either<DeleteChangeProposalError, Unit>

    /**
     * Check if a change proposal exists.
     *
     * @param id The proposal identifier
     * @return Either an error or boolean indicating existence
     */
    suspend fun existsById(id: ProposalId): Either<ExistsChangeProposalError, Boolean>

    /**
     * Count change proposals by state.
     *
     * @param state The proposal state
     * @return Either an error or the count
     */
    suspend fun countByState(state: ProposalState): Either<FindChangeProposalError, Long>

    /**
     * Count active proposals for a resource (not in terminal states).
     *
     * @param resourceId The resource identifier
     * @return Either an error or the count
     */
    suspend fun countActiveByResourceId(resourceId: ResourceId): Either<FindChangeProposalError, Long>

    /**
     * Check if there are any proposals in a non-terminal state for a resource.
     *
     * @param resourceId The resource identifier
     * @return Either an error or boolean indicating if active proposals exist
     */
    suspend fun hasActiveProposals(resourceId: ResourceId): Either<FindChangeProposalError, Boolean>
}
