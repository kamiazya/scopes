package io.github.kamiazya.scopes.collaborativeversioning.domain.model

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.ProposedChange
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.ReviewComment
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.ReviewCommentType
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.ChangeProposalError
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Aggregate root for managing change proposals.
 *
 * ChangeProposal manages the lifecycle of proposed changes from creation through review
 * to approval and application. It enforces business rules around state transitions,
 * review processes, and conflict detection.
 *
 * This aggregate is immutable - all operations return a new instance.
 */
data class ChangeProposal private constructor(
    val id: ProposalId,
    val author: Author,
    val state: ProposalState,
    val proposedChanges: List<ProposedChange>,
    val reviewComments: List<ReviewComment>,
    val targetResourceId: ResourceId,
    val title: String,
    val description: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val submittedAt: Instant? = null,
    val reviewStartedAt: Instant? = null,
    val resolvedAt: Instant? = null,
    val appliedAt: Instant? = null,
    val rejectionReason: String? = null,
) {
    companion object {
        /**
         * Create a new change proposal in DRAFT state.
         */
        fun create(
            author: Author,
            targetResourceId: ResourceId,
            title: String,
            description: String,
            proposedChanges: List<ProposedChange> = emptyList(),
            timestamp: Instant = Clock.System.now(),
        ): Either<ChangeProposalError, ChangeProposal> = either {
            ensure(title.isNotBlank()) {
                ChangeProposalError.EmptyTitle
            }
            ensure(description.isNotBlank()) {
                ChangeProposalError.EmptyDescription
            }

            ChangeProposal(
                id = ProposalId.generate(),
                author = author,
                state = ProposalState.DRAFT,
                proposedChanges = proposedChanges,
                reviewComments = emptyList(),
                targetResourceId = targetResourceId,
                title = title,
                description = description,
                createdAt = timestamp,
                updatedAt = timestamp,
            )
        }
    }

    /**
     * Add a proposed change to the proposal.
     * Only allowed in DRAFT state.
     * Returns a new instance with the change added.
     */
    fun addProposedChange(proposedChange: ProposedChange, timestamp: Instant = Clock.System.now()): Either<ChangeProposalError, ChangeProposal> = either {
        ensure(state.isEditable()) {
            ChangeProposalError.InvalidStateTransition(state, "add proposed change")
        }
        ensure(proposedChange.resourceId == targetResourceId) {
            ChangeProposalError.ResourceMismatch(targetResourceId, proposedChange.resourceId)
        }

        copy(
            proposedChanges = proposedChanges + proposedChange,
            updatedAt = timestamp,
        )
    }

    /**
     * Remove a proposed change from the proposal.
     * Only allowed in DRAFT state.
     * Returns a new instance with the change removed.
     */
    fun removeProposedChange(proposedChangeId: String, timestamp: Instant = Clock.System.now()): Either<ChangeProposalError, ChangeProposal> = either {
        ensure(state.isEditable()) {
            ChangeProposalError.InvalidStateTransition(state, "remove proposed change")
        }

        val filtered = proposedChanges.filter { it.id != proposedChangeId }
        ensure(filtered.size < proposedChanges.size) {
            ChangeProposalError.ProposedChangeNotFound(proposedChangeId)
        }

        copy(
            proposedChanges = filtered,
            updatedAt = timestamp,
        )
    }

    /**
     * Submit the proposal for review.
     * Transitions from DRAFT to SUBMITTED.
     * Returns a new instance in SUBMITTED state.
     */
    fun submit(timestamp: Instant = Clock.System.now()): Either<ChangeProposalError, ChangeProposal> = either {
        ensure(proposedChanges.isNotEmpty()) {
            ChangeProposalError.NoProposedChanges
        }
        ensure(state.canTransitionTo(ProposalState.SUBMITTED)) {
            ChangeProposalError.InvalidStateTransition(state, ProposalState.SUBMITTED.name)
        }

        copy(
            state = ProposalState.SUBMITTED,
            submittedAt = timestamp,
            updatedAt = timestamp,
        )
    }

    /**
     * Start the review process.
     * Transitions from SUBMITTED to REVIEWING.
     * Returns a new instance in REVIEWING state.
     */
    fun startReview(timestamp: Instant = Clock.System.now()): Either<ChangeProposalError, ChangeProposal> = either {
        ensure(state.canTransitionTo(ProposalState.REVIEWING)) {
            ChangeProposalError.InvalidStateTransition(state, ProposalState.REVIEWING.name)
        }

        copy(
            state = ProposalState.REVIEWING,
            reviewStartedAt = timestamp,
            updatedAt = timestamp,
        )
    }

    /**
     * Add a review comment to the proposal.
     * Allowed in REVIEWING and APPROVED states.
     * Returns a new instance with the comment added.
     */
    fun addReviewComment(comment: ReviewComment, timestamp: Instant = Clock.System.now()): Either<ChangeProposalError, ChangeProposal> = either {
        ensure(state.canReceiveComments()) {
            ChangeProposalError.InvalidStateTransition(state, "add review comment")
        }

        // Validate proposed change ID if specified
        if (comment.proposedChangeId != null) {
            ensure(proposedChanges.any { it.id == comment.proposedChangeId }) {
                ChangeProposalError.ProposedChangeNotFound(comment.proposedChangeId)
            }
        }

        // Validate parent comment ID if specified
        if (comment.parentCommentId != null) {
            ensure(reviewComments.any { it.id == comment.parentCommentId }) {
                ChangeProposalError.ParentCommentNotFound(comment.parentCommentId)
            }
        }

        copy(
            reviewComments = reviewComments + comment,
            updatedAt = timestamp,
        )
    }

    /**
     * Approve the proposal.
     * Transitions from REVIEWING to APPROVED.
     * Returns a new instance in APPROVED state.
     */
    fun approve(approver: Author, message: String? = null, timestamp: Instant = Clock.System.now()): Either<ChangeProposalError, ChangeProposal> = either {
        ensure(state.canTransitionTo(ProposalState.APPROVED)) {
            ChangeProposalError.InvalidStateTransition(state, ProposalState.APPROVED.name)
        }

        // Add approval comment if message provided
        val updatedComments = if (message != null) {
            val approvalComment = ReviewComment.create(
                author = approver,
                content = message,
                commentType = ReviewCommentType.APPROVAL,
                timestamp = timestamp,
            )
            reviewComments + approvalComment
        } else {
            reviewComments
        }

        copy(
            state = ProposalState.APPROVED,
            reviewComments = updatedComments,
            resolvedAt = timestamp,
            updatedAt = timestamp,
        )
    }

    /**
     * Reject the proposal.
     * Transitions from REVIEWING to REJECTED.
     * Returns a new instance in REJECTED state.
     */
    fun reject(reviewer: Author, reason: String, timestamp: Instant = Clock.System.now()): Either<ChangeProposalError, ChangeProposal> = either {
        ensure(state.canTransitionTo(ProposalState.REJECTED)) {
            ChangeProposalError.InvalidStateTransition(state, ProposalState.REJECTED.name)
        }
        ensure(reason.isNotBlank()) {
            ChangeProposalError.EmptyRejectionReason
        }

        // Add rejection comment
        val rejectionComment = ReviewComment.create(
            author = reviewer,
            content = reason,
            commentType = ReviewCommentType.REQUEST_CHANGES,
            timestamp = timestamp,
        )

        copy(
            state = ProposalState.REJECTED,
            reviewComments = reviewComments + rejectionComment,
            rejectionReason = reason,
            resolvedAt = timestamp,
            updatedAt = timestamp,
        )
    }

    /**
     * Apply the approved changes.
     * Transitions from APPROVED to APPLIED.
     * Returns a tuple of the new instance and the applied changes.
     */
    fun apply(timestamp: Instant = Clock.System.now()): Either<ChangeProposalError, Pair<ChangeProposal, AppliedChanges>> = either {
        ensure(state.canTransitionTo(ProposalState.APPLIED)) {
            ChangeProposalError.InvalidStateTransition(state, ProposalState.APPLIED.name)
        }

        val newProposal = copy(
            state = ProposalState.APPLIED,
            appliedAt = timestamp,
            updatedAt = timestamp,
        )

        val appliedChanges = AppliedChanges(
            proposalId = id,
            proposedChanges = proposedChanges,
            appliedAt = timestamp,
        )

        Pair(newProposal, appliedChanges)
    }

    /**
     * Detect conflicts with the current state of the target resource.
     * This should be called before applying the proposal.
     */
    fun detectConflicts(currentResourceState: ResourceState): List<Conflict> {
        val conflicts = mutableListOf<Conflict>()

        // Check each proposed change for conflicts
        proposedChanges.forEach { proposedChange ->
            when (proposedChange) {
                is ProposedChange.Inline -> {
                    // Check if any of the inline changes conflict with current state
                    proposedChange.changes.forEach { change ->
                        val currentValue = currentResourceState.getValueAtPath(change.path)
                        if (currentValue != change.previousValue) {
                            conflicts.add(
                                Conflict(
                                    proposedChangeId = proposedChange.id,
                                    path = change.path,
                                    expectedValue = change.previousValue,
                                    actualValue = currentValue,
                                    description = "The resource has been modified since this change was proposed",
                                ),
                            )
                        }
                    }
                }
                is ProposedChange.FromChangeset -> {
                    // Changeset conflicts would be detected when applying the changeset
                    // This is a simplified check
                    if (!currentResourceState.canApplyChangeset(proposedChange.changesetId)) {
                        conflicts.add(
                            Conflict(
                                proposedChangeId = proposedChange.id,
                                path = "changeset",
                                expectedValue = null,
                                actualValue = null,
                                description = "The changeset cannot be applied to the current resource state",
                            ),
                        )
                    }
                }
            }
        }

        return conflicts
    }

    /**
     * Check if there are any unresolved review comments that are issues.
     */
    fun hasUnresolvedIssues(): Boolean = reviewComments.any {
        it.commentType == ReviewCommentType.ISSUE && !it.resolved
    }

    /**
     * Resolve a review comment.
     * Returns a new instance with the comment resolved.
     */
    fun resolveComment(commentId: String, resolver: Author, timestamp: Instant = Clock.System.now()): Either<ChangeProposalError, ChangeProposal> = either {
        val commentIndex = reviewComments.indexOfFirst { it.id == commentId }
        ensure(commentIndex >= 0) {
            ChangeProposalError.ParentCommentNotFound(commentId)
        }

        val updatedComments = reviewComments.toMutableList()
        updatedComments[commentIndex] = reviewComments[commentIndex].resolve(resolver, timestamp)

        copy(
            reviewComments = updatedComments,
            updatedAt = timestamp,
        )
    }

    /**
     * Get statistics about the proposal.
     */
    fun getStatistics(): ProposalStatistics = ProposalStatistics(
        proposedChangeCount = proposedChanges.size,
        totalCommentCount = reviewComments.size,
        unresolvedCommentCount = reviewComments.count { !it.resolved },
        issueCount = reviewComments.count { it.commentType == ReviewCommentType.ISSUE },
        unresolvedIssueCount = reviewComments.count { it.commentType == ReviewCommentType.ISSUE && !it.resolved },
    )
}

/**
 * Result of applying a change proposal.
 */
data class AppliedChanges(val proposalId: ProposalId, val proposedChanges: List<ProposedChange>, val appliedAt: Instant)

/**
 * Represents a conflict between a proposed change and the current resource state.
 */
data class Conflict(val proposedChangeId: String, val path: String, val expectedValue: String?, val actualValue: String?, val description: String)

/**
 * Represents the current state of a resource for conflict detection.
 * This is a simplified interface - actual implementation would depend on resource type.
 */
interface ResourceState {
    fun getValueAtPath(path: String): String?
    fun canApplyChangeset(changesetId: ChangesetId): Boolean
}

/**
 * Statistics about a change proposal.
 */
data class ProposalStatistics(
    val proposedChangeCount: Int,
    val totalCommentCount: Int,
    val unresolvedCommentCount: Int,
    val issueCount: Int,
    val unresolvedIssueCount: Int,
)
