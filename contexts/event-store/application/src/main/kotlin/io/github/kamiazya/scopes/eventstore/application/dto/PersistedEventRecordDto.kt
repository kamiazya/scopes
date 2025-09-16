package io.github.kamiazya.scopes.eventstore.application.dto

import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import kotlinx.datetime.Instant

/**
 * DTO representation of a stored event.
 */
data class PersistedEventRecordDto(
    val eventId: String,
    val aggregateId: String,
    val aggregateVersion: Long,
    val eventType: String,
    val storedAt: Instant,
    val sequenceNumber: Long,
    val event: DomainEvent? = null, // Optional to maintain backward compatibility
)
