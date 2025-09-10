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

/**
 * Command to start the review process for a change proposal.
 *
 * This transitions the proposal from SUBMITTED to REVIEWING state.
 */
data class StartReviewCommand(val proposalId: ProposalId)

/**
 * Command to resolve a review comment.
 *
 * This marks a specific review comment as resolved.
 */
data class ResolveCommentCommand(val proposalId: ProposalId, val commentId: String, val resolver: Author)
