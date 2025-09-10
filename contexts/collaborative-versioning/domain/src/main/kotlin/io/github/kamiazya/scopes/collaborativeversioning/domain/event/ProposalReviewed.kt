package io.github.kamiazya.scopes.collaborativeversioning.domain.event

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.event.EventMetadata
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import kotlinx.datetime.Instant

/**
 * Event emitted when a review comment is added to a proposal.
 *
 * This event captures feedback and review decisions during the
 * REVIEWING state of a proposal.
 */
data class ProposalReviewed(
    override val eventId: EventId,
    override val aggregateId: AggregateId,
    override val aggregateVersion: AggregateVersion,
    override val occurredAt: Instant,
    override val metadata: EventMetadata? = null,

    /**
     * The unique identifier of the reviewed proposal.
     */
    val proposalId: ProposalId,

    /**
     * Type of review (e.g., "comment", "approval", "request_changes").
     */
    val reviewType: ReviewType,

    /**
     * The review comment or feedback.
     */
    val comment: String,

    /**
     * ID of the user who performed the review.
     */
    val reviewedBy: String,

    /**
     * Optional severity level for the review (e.g., for issues found).
     */
    val severity: ReviewSeverity? = null,

    /**
     * Optional line numbers or sections referenced in the review.
     */
    val references: List<String> = emptyList(),
) : DomainEvent {

    companion object {
        /**
         * The stable type identifier for this event.
         */
        const val TYPE_ID = "collaborative-versioning.proposal.reviewed.v1"
    }
}

/**
 * Types of reviews that can be performed on a proposal.
 */
enum class ReviewType {
    /**
     * General comment without explicit approval/rejection.
     */
    COMMENT,

    /**
     * Explicit approval of the proposal.
     */
    APPROVAL,

    /**
     * Request for changes before approval.
     */
    REQUEST_CHANGES,

    /**
     * Question needing clarification.
     */
    QUESTION,
}

/**
 * Severity levels for review feedback.
 */
enum class ReviewSeverity {
    /**
     * Information only, no action required.
     */
    INFO,

    /**
     * Minor issue or suggestion.
     */
    MINOR,

    /**
     * Major issue that should be addressed.
     */
    MAJOR,

    /**
     * Critical issue that must be resolved.
     */
    CRITICAL,
}
