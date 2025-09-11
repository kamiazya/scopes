package io.github.kamiazya.scopes.collaborativeversioning.application.command

import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.ReviewComment
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.Author
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId

/**
 * Command to add a review comment to a change proposal.
 *
 * This command allows reviewers to add comments, feedback, or issues
 * to a change proposal during the review process.
 */
data class ReviewProposalCommand(val proposalId: ProposalId, val reviewer: Author, val comment: ReviewComment)
