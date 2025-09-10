package io.github.kamiazya.scopes.collaborativeversioning.infrastructure.event

import io.github.kamiazya.scopes.collaborativeversioning.domain.event.*
import io.github.kamiazya.scopes.eventstore.domain.model.EventTypeMapping

/**
 * Registers all collaborative versioning domain events with the event store.
 *
 * This registration ensures that events can be properly serialized and
 * deserialized when stored and retrieved from the event store.
 */
object CollaborativeVersioningEventTypeRegistration {

    /**
     * Register all collaborative versioning events with the provided mapping.
     */
    fun registerAll(mapping: EventTypeMapping) {
        mapping.apply {
            // Register each event type with its stable identifier
            register(ProposalCreated::class, ProposalCreated.TYPE_ID)
            register(ProposalUpdated::class, ProposalUpdated.TYPE_ID)
            register(ProposalReviewed::class, ProposalReviewed.TYPE_ID)
            register(ProposalApproved::class, ProposalApproved.TYPE_ID)
            register(ChangeMerged::class, ChangeMerged.TYPE_ID)
            register(ConflictDetected::class, ConflictDetected.TYPE_ID)
        }
    }

    /**
     * List of all event type IDs for verification and documentation.
     */
    val ALL_TYPE_IDS = listOf(
        ProposalCreated.TYPE_ID,
        ProposalUpdated.TYPE_ID,
        ProposalReviewed.TYPE_ID,
        ProposalApproved.TYPE_ID,
        ChangeMerged.TYPE_ID,
        ConflictDetected.TYPE_ID,
    )
}
