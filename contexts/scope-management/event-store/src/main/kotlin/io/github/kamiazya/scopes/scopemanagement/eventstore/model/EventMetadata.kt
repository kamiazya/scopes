package io.github.kamiazya.scopes.scopemanagement.eventstore.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Metadata associated with a stored event.
 *
 * This contains information about when and where the event was created,
 * as well as vector clock information for synchronization.
 */
@Serializable
data class EventMetadata(
    val eventId: String,
    val aggregateId: String,
    val eventType: String,
    val deviceId: String,
    val vectorClock: VectorClock,
    val timestamp: Instant,
    val sequenceNumber: Long,
)
