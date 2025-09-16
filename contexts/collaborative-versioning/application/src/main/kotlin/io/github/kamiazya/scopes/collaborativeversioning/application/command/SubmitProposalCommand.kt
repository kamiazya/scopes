package io.github.kamiazya.scopes.collaborativeversioning.application.command

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId

/**
 * Command to submit a proposal for review.
 *
 * This transitions a proposal from DRAFT to SUBMITTED state.
 */
data class SubmitProposalCommand(val proposalId: ProposalId)
