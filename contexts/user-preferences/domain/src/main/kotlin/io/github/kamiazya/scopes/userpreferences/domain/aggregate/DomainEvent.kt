package io.github.kamiazya.scopes.userpreferences.domain.aggregate

import kotlinx.datetime.Instant

interface DomainEvent {
    val eventId: EventId
    val aggregateId: AggregateId
    val occurredAt: Instant
}
