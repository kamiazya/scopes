package io.github.kamiazya.scopes.eventstore.application.dto

import kotlinx.datetime.Instant

/**
 * DTO representation of a stored event.
 */
data class PersistedEventRecordDto(
    val eventId: String,
    val aggregateId: String,
    val aggregateVersion: Long,
    val eventType: String,
    val occurredAt: Instant,
    val storedAt: Instant,
    val sequenceNumber: Long,
)
