package io.github.kamiazya.scopes.contracts.eventstore.commands

import kotlinx.datetime.Instant

/**
 * Command to store an event in the event store.
 */
public data class StoreEventCommand(
    val aggregateId: String,
    val aggregateVersion: Long,
    val eventType: String,
    val eventData: String, // JSON serialized event data
    val occurredAt: Instant,
    val metadata: Map<String, String> = emptyMap(),
)
