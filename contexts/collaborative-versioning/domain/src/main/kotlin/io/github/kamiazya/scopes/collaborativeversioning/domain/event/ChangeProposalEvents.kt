package io.github.kamiazya.scopes.collaborativeversioning.domain.event

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.*
import kotlinx.datetime.Instant

/**
 * Base class for all ChangeProposal domain events.
 */
sealed class ChangeProposalEvent : DomainEvent {
    abstract val proposalId: ProposalId
    abstract val resourceId: ResourceId
    abstract override val occurredAt: Instant
}

/**
 * Event raised when a new change proposal is created.
 */
data class ChangeProposalCreated(
    override val proposalId: ProposalId,
    override val resourceId: ResourceId,
    val author: Author,
    val title: String,
    val description: String,
    val proposedChangeCount: Int,
    override val occurredAt: Instant,
) : ChangeProposalEvent()

/**
 * Event raised when a change proposal is submitted for review.
 */
data class ChangeProposalSubmitted(
    override val proposalId: ProposalId,
    override val resourceId: ResourceId,
    val author: Author,
    val proposedChangeCount: Int,
    override val occurredAt: Instant,
) : ChangeProposalEvent()

/**
 * Event raised when review of a change proposal starts.
 */
data class ChangeProposalReviewStarted(override val proposalId: ProposalId, override val resourceId: ResourceId, override val occurredAt: Instant) :
    ChangeProposalEvent()

/**
 * Event raised when a proposed change is added to a proposal.
 */
data class ProposedChangeAdded(
    override val proposalId: ProposalId,
    override val resourceId: ResourceId,
    val proposedChangeId: String,
    val changeType: String, // "inline" or "from_changeset"
    override val occurredAt: Instant,
) : ChangeProposalEvent()

/**
 * Event raised when a proposed change is removed from a proposal.
 */
data class ProposedChangeRemoved(
    override val proposalId: ProposalId,
    override val resourceId: ResourceId,
    val proposedChangeId: String,
    override val occurredAt: Instant,
) : ChangeProposalEvent()

/**
 * Event raised when a review comment is added to a proposal.
 */
data class ReviewCommentAdded(
    override val proposalId: ProposalId,
    override val resourceId: ResourceId,
    val commentId: String,
    val author: Author,
    val commentType: String,
    val proposedChangeId: String? = null,
    val parentCommentId: String? = null,
    override val occurredAt: Instant,
) : ChangeProposalEvent()

/**
 * Event raised when a change proposal is approved.
 */
data class ChangeProposalApproved(
    override val proposalId: ProposalId,
    override val resourceId: ResourceId,
    val approver: Author,
    val message: String? = null,
    override val occurredAt: Instant,
) : ChangeProposalEvent()

/**
 * Event raised when a change proposal is rejected.
 */
data class ChangeProposalRejected(
    override val proposalId: ProposalId,
    override val resourceId: ResourceId,
    val reviewer: Author,
    val reason: String,
    override val occurredAt: Instant,
) : ChangeProposalEvent()

/**
 * Event raised when a change proposal is applied.
 */
data class ChangeProposalApplied(
    override val proposalId: ProposalId,
    override val resourceId: ResourceId,
    val appliedChangeCount: Int,
    override val occurredAt: Instant,
) : ChangeProposalEvent()

/**
 * Event raised when conflicts are detected in a change proposal.
 */
data class ChangeProposalConflictsDetected(
    override val proposalId: ProposalId,
    override val resourceId: ResourceId,
    val conflictCount: Int,
    val conflictPaths: List<String>,
    override val occurredAt: Instant,
) : ChangeProposalEvent()

/**
 * Event raised when a review comment is resolved.
 */
data class ReviewCommentResolved(
    override val proposalId: ProposalId,
    override val resourceId: ResourceId,
    val commentId: String,
    val resolver: Author,
    override val occurredAt: Instant,
) : ChangeProposalEvent()
