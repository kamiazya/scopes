package io.github.kamiazya.scopes.contracts.eventstore.results

import kotlinx.datetime.Instant

/**
 * Result of an event store operation.
 */
public data class EventResult(
    val eventId: String,
    val aggregateId: String,
    val aggregateVersion: Long,
    val eventType: String,
    val eventData: String, // JSON serialized event data
    val storedAt: Instant,
    val sequenceNumber: Long,
    val metadata: Map<String, String> = emptyMap(),
)
