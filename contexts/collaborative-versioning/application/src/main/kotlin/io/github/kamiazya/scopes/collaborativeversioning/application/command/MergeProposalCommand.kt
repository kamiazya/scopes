package io.github.kamiazya.scopes.collaborativeversioning.application.command

import io.github.kamiazya.scopes.collaborativeversioning.domain.model.ResourceState
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.Author
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId

/**
 * Command to merge/apply an approved change proposal.
 *
 * This command applies the approved changes to the target resource
 * and transitions the proposal to APPLIED state.
 */
data class MergeProposalCommand(val proposalId: ProposalId, val applicator: Author, val currentResourceState: ResourceState)

/**
 * Command to submit a proposal for review.
 *
 * This transitions a proposal from DRAFT to SUBMITTED state.
 */
data class SubmitProposalCommand(val proposalId: ProposalId)
