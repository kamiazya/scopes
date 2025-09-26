package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import io.github.kamiazya.scopes.scopemanagement.db.Event_outbox
import io.github.kamiazya.scopes.scopemanagement.db.ScopeManagementDatabase
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SqlDelightEventOutboxRepository(private val database: ScopeManagementDatabase) {
    fun enqueue(id: String, eventId: String, aggregateId: String, aggregateVersion: Long, eventType: String, payload: String, occurredAt: Instant) {
        val now = Clock.System.now()
        database.eventOutboxQueries.enqueueOutbox(
            id = id,
            event_id = eventId,
            aggregate_id = aggregateId,
            aggregate_version = aggregateVersion,
            event_type = eventType,
            payload = payload,
            occurred_at = occurredAt.toEpochMilliseconds(),
            created_at = now.toEpochMilliseconds(),
        )
    }

    fun fetchPending(limit: Int): List<Event_outbox> = database.eventOutboxQueries.fetchPending(limit.toLong()).executeAsList()

    fun markProcessed(id: String, processedAt: Instant) {
        database.eventOutboxQueries.markProcessed(
            processed_at = processedAt.toEpochMilliseconds(),
            id = id,
        )
    }

    fun markFailed(id: String) {
        database.eventOutboxQueries.markFailed(id)
    }
}
