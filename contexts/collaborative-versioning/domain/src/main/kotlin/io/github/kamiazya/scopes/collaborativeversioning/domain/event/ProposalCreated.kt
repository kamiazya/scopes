package io.github.kamiazya.scopes.collaborativeversioning.domain.event

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.event.EventMetadata
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import kotlinx.datetime.Instant

/**
 * Event emitted when a new change proposal is created.
 *
 * This event captures the creation of a proposal in the DRAFT state,
 * including all initial properties and metadata about the creation context.
 */
data class ProposalCreated(
    override val eventId: EventId,
    override val aggregateId: AggregateId,
    override val aggregateVersion: AggregateVersion,
    override val occurredAt: Instant,
    override val metadata: EventMetadata? = null,

    /**
     * The unique identifier of the created proposal.
     */
    val proposalId: ProposalId,

    /**
     * Title of the proposal describing the change.
     */
    val title: String,

    /**
     * Detailed description of the proposed changes.
     */
    val description: String,

    /**
     * The scope ID that this proposal targets.
     */
    val targetScopeId: String,

    /**
     * The type of change being proposed (e.g., "update_title", "update_description").
     */
    val changeType: String,

    /**
     * The proposed change details as a structured payload.
     * Structure depends on the changeType.
     */
    val changePayload: Map<String, Any>,

    /**
     * ID of the user who created the proposal.
     */
    val createdBy: String,

    /**
     * Optional tags for categorizing the proposal.
     */
    val tags: List<String> = emptyList(),
) : DomainEvent {

    companion object {
        /**
         * The stable type identifier for this event.
         * Used for event store persistence and deserialization.
         */
        const val TYPE_ID = "collaborative-versioning.proposal.created.v1"
    }
}
