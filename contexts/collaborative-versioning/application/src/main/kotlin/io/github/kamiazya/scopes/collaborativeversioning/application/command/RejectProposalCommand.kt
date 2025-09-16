package io.github.kamiazya.scopes.collaborativeversioning.application.command

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.Author
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId

/**
 * Command to reject a change proposal.
 *
 * This transitions the proposal from REVIEWING to REJECTED state.
 */
data class RejectProposalCommand(val proposalId: ProposalId, val reviewer: Author, val rejectionReason: String)
