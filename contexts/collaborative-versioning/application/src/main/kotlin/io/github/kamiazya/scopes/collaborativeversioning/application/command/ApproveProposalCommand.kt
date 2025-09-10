package io.github.kamiazya.scopes.collaborativeversioning.application.command

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.Author
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId

/**
 * Command to approve a change proposal.
 *
 * This transitions the proposal from REVIEWING to APPROVED state,
 * making it ready for merging/application.
 */
data class ApproveProposalCommand(val proposalId: ProposalId, val approver: Author, val approvalMessage: String? = null)

/**
 * Command to reject a change proposal.
 *
 * This transitions the proposal from REVIEWING to REJECTED state.
 */
data class RejectProposalCommand(val proposalId: ProposalId, val reviewer: Author, val rejectionReason: String)
