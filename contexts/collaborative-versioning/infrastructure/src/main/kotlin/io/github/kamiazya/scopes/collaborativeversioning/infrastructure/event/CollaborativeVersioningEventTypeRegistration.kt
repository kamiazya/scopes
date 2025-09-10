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
     * List of all collaborative versioning event types.
     */
    private val EVENT_CLASSES = listOf(
        ProposalCreated::class,
        ProposalUpdated::class,
        ProposalReviewed::class,
        ProposalApproved::class,
        ChangeMerged::class,
        ConflictDetected::class,
    )

    /**
     * Register all collaborative versioning events with the provided mapping.
     * The mapping will automatically use @EventTypeId annotations on each event class.
     */
    fun registerAll(mapping: EventTypeMapping) {
        EVENT_CLASSES.forEach { eventClass ->
            // The mapping will automatically detect and use @EventTypeId annotation
            val typeId = mapping.getTypeId(eventClass)
            // This ensures the event is cached in the mapping
        }
    }

    /**
     * List of all event type IDs for verification and documentation.
     * This is dynamically generated from the event classes.
     */
    val ALL_TYPE_IDS: List<String> by lazy {
        EVENT_CLASSES.map { eventClass ->
            // Extract the EventTypeId annotation value
            eventClass.annotations
                .filterIsInstance<io.github.kamiazya.scopes.eventstore.domain.valueobject.EventTypeId>()
                .firstOrNull()?.value
                ?: error("Event class ${eventClass.simpleName} must have @EventTypeId annotation")
        }
    }
}
