package io.github.kamiazya.scopes.eventstore.domain.valueobject

import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import kotlinx.datetime.Instant

/**
 * Metadata associated with a stored event.
 *
 * @property eventId The unique identifier of the event
 * @property aggregateId The ID of the aggregate this event belongs to
 * @property aggregateVersion The version of the aggregate after this event
 * @property eventType The type/class name of the event
 * @property storedAt When the event was stored
 * @property sequenceNumber The sequence number of this event in the store
 */
data class EventMetadata(
    val eventId: EventId,
    val aggregateId: AggregateId,
    val aggregateVersion: AggregateVersion,
    val eventType: EventType,
    val storedAt: Instant,
    val sequenceNumber: Long,
) {
    init {
        require(sequenceNumber >= 0) { "Sequence number must be non-negative" }
    }
}
