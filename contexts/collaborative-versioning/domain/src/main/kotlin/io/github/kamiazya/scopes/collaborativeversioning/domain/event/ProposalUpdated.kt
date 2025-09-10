package io.github.kamiazya.scopes.collaborativeversioning.domain.event

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId
import io.github.kamiazya.scopes.eventstore.domain.valueobject.EventTypeId
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.event.EventMetadata
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import kotlinx.datetime.Instant

/**
 * Event emitted when a proposal is updated while in DRAFT state.
 *
 * This event captures modifications to a proposal's properties
 * before it has been submitted for review.
 */
@EventTypeId("collaborative-versioning.proposal.updated.v1")
data class ProposalUpdated(
    override val eventId: EventId,
    override val aggregateId: AggregateId,
    override val aggregateVersion: AggregateVersion,
    override val occurredAt: Instant,
    override val metadata: EventMetadata? = null,

    /**
     * The unique identifier of the updated proposal.
     */
    val proposalId: ProposalId,

    /**
     * Fields that were updated in this change.
     * Keys are field names, values are the new values.
     */
    val updatedFields: Map<String, Any>,

    /**
     * Previous values of the updated fields for audit purposes.
     * Keys match those in updatedFields.
     */
    val previousValues: Map<String, Any>,

    /**
     * ID of the user who performed the update.
     */
    val updatedBy: String,

    /**
     * Optional reason for the update.
     */
    val updateReason: String? = null,
) : DomainEvent
