package io.github.kamiazya.scopes.collaborativeversioning.application.dto

import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.ReviewComment
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.ChangeProposal
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.AppliedChanges
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalConflict
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalState
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalStatistics
import kotlinx.datetime.Instant

/**
 * Result of creating a new change proposal.
 */
data class ProposeChangeResultDto(
    val proposalId: ProposalId,
    val state: ProposalState,
    val title: String,
    val description: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(proposal: ChangeProposal): ProposeChangeResultDto = ProposeChangeResultDto(
            proposalId = proposal.id,
            state = proposal.state,
            title = proposal.title,
            description = proposal.description,
            createdAt = proposal.createdAt,
            updatedAt = proposal.updatedAt,
        )
    }
}

/**
 * Result of reviewing a proposal (adding comments, starting review, etc.).
 */
data class ReviewProposalResultDto(
    val proposalId: ProposalId,
    val state: ProposalState,
    val reviewComments: List<ReviewComment>,
    val statistics: ProposalStatistics,
    val updatedAt: Instant,
) {
    companion object {
        fun from(proposal: ChangeProposal): ReviewProposalResultDto = ReviewProposalResultDto(
            proposalId = proposal.id,
            state = proposal.state,
            reviewComments = proposal.reviewComments,
            statistics = proposal.getStatistics(),
            updatedAt = proposal.updatedAt,
        )
    }
}

/**
 * Result of approving a proposal.
 */
data class ApproveProposalResultDto(
    val proposalId: ProposalId,
    val state: ProposalState,
    val resolvedAt: Instant?,
    val hasUnresolvedIssues: Boolean,
    val updatedAt: Instant,
) {
    companion object {
        fun from(proposal: ChangeProposal): ApproveProposalResultDto = ApproveProposalResultDto(
            proposalId = proposal.id,
            state = proposal.state,
            resolvedAt = proposal.resolvedAt,
            hasUnresolvedIssues = proposal.hasUnresolvedIssues(),
            updatedAt = proposal.updatedAt,
        )
    }
}

/**
 * Result of merging/applying a proposal.
 */
data class MergeProposalResultDto(
    val proposalId: ProposalId,
    val state: ProposalState,
    val appliedChanges: AppliedChanges,
    val conflicts: List<ProposalConflict>,
    val appliedAt: Instant?,
    val updatedAt: Instant,
) {
    companion object {
        fun from(proposal: ChangeProposal, appliedChanges: AppliedChanges, conflicts: List<ProposalConflict> = emptyList()): MergeProposalResultDto =
            MergeProposalResultDto(
                proposalId = proposal.id,
                state = proposal.state,
                appliedChanges = appliedChanges,
                conflicts = conflicts,
                appliedAt = proposal.appliedAt,
                updatedAt = proposal.updatedAt,
            )
    }
}

/**
 * Result of submitting a proposal for review.
 */
data class SubmitProposalResultDto(val proposalId: ProposalId, val state: ProposalState, val submittedAt: Instant?, val updatedAt: Instant) {
    companion object {
        fun from(proposal: ChangeProposal): SubmitProposalResultDto = SubmitProposalResultDto(
            proposalId = proposal.id,
            state = proposal.state,
            submittedAt = proposal.submittedAt,
            updatedAt = proposal.updatedAt,
        )
    }
}
