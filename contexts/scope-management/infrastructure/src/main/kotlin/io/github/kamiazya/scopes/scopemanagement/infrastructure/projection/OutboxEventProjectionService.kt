package io.github.kamiazya.scopes.scopemanagement.infrastructure.projection

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.commons.id.ULID
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.EventPublisher
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightEventOutboxRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * EventPublisher implementation that enqueues events into an outbox table.
 * Optionally processes them immediately to preserve current synchronous behavior.
 */
class OutboxEventProjectionService(
    private val outboxRepository: SqlDelightEventOutboxRepository,
    private val projector: OutboxProjectionService,
    private val json: Json,
    private val logger: Logger,
    private val processImmediately: Boolean = true,
) : EventPublisher {

    override suspend fun projectEvent(event: DomainEvent): Either<ScopeManagementApplicationError, Unit> = projectEvents(listOf(event))

    override suspend fun projectEvents(events: List<DomainEvent>): Either<ScopeManagementApplicationError, Unit> = try {
        val ids = mutableListOf<String>()
        events.forEach { event ->
            val id = ULID.generate().value
            ids += id
            val payload = json.encodeToString(event)
            val type = eventTypeId(event)
            outboxRepository.enqueue(
                id = id,
                eventId = event.eventId.value,
                aggregateId = event.aggregateId.value,
                aggregateVersion = event.aggregateVersion.value,
                eventType = type,
                payload = payload,
                occurredAt = event.occurredAt,
            )
        }
        if (processImmediately) {
            projector.processByIds(ids)
        }
        Unit.right()
    } catch (e: Exception) {
        logger.error("Failed to enqueue events to outbox", mapOf("error" to e.message.orEmpty()))
        ScopeManagementApplicationError.PersistenceError.ProjectionFailed(
            eventType = "batch",
            aggregateId = "n/a",
            reason = e.message ?: "(no_message)",
        ).left()
    }

    private fun eventTypeId(event: DomainEvent): String {
        val ann = event::class.annotations.firstOrNull { it is io.github.kamiazya.scopes.platform.domain.event.EventTypeId }
        if (ann is io.github.kamiazya.scopes.platform.domain.event.EventTypeId) return ann.value
        return event::class.qualifiedName ?: (event::class.simpleName ?: "UnknownEvent")
    }

    // Expose refresh capability for eventual consistency checks
    suspend fun refresh(): Either<io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError, Unit> =
        projector.processPending()
}
