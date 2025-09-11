package io.github.kamiazya.scopes.collaborativeversioning.application.command

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId

/**
 * Command to start the review process for a change proposal.
 *
 * This transitions the proposal from SUBMITTED to REVIEWING state.
 */
data class StartReviewCommand(val proposalId: ProposalId)
