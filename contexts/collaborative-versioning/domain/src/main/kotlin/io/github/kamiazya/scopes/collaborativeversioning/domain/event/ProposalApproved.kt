package io.github.kamiazya.scopes.collaborativeversioning.domain.event

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.event.EventMetadata
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import kotlinx.datetime.Instant

/**
 * Event emitted when a proposal is approved and ready to be applied.
 *
 * This event marks the transition from REVIEWING to APPROVED state,
 * indicating that the proposal has passed all reviews and is ready
 * to be merged/applied to the target resource.
 */
data class ProposalApproved(
    override val eventId: EventId,
    override val aggregateId: AggregateId,
    override val aggregateVersion: AggregateVersion,
    override val occurredAt: Instant,
    override val metadata: EventMetadata? = null,

    /**
     * The unique identifier of the approved proposal.
     */
    val proposalId: ProposalId,

    /**
     * ID of the user who gave final approval.
     */
    val approvedBy: String,

    /**
     * List of all reviewers who approved the proposal.
     */
    val approvers: List<String>,

    /**
     * Optional approval comment or rationale.
     */
    val approvalComment: String? = null,

    /**
     * Conditions that must be met before applying (if any).
     */
    val conditions: List<String> = emptyList(),

    /**
     * Scheduled time for automatic application (if set).
     */
    val scheduledApplicationTime: Instant? = null,
) : DomainEvent {

    companion object {
        /**
         * The stable type identifier for this event.
         */
        const val TYPE_ID = "collaborative-versioning.proposal.approved.v1"
    }
}
