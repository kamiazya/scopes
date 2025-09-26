package io.github.kamiazya.scopes.scopemanagement.infrastructure.projection

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightEventOutboxRepository
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class OutboxProjectionService(
    private val outboxRepository: SqlDelightEventOutboxRepository,
    private val projectionService: EventProjectionService,
    private val json: Json,
    private val logger: Logger,
) {
    suspend fun processPending(batchSize: Int = 200): Either<ScopeManagementApplicationError, Unit> = either {
        val pending = outboxRepository.fetchPending(batchSize)
        if (pending.isEmpty()) return@either
        processRecords(pending.map { it.id }).bind()
    }

    // Expose a refresh API for eventual consistency tests
    suspend fun refreshPending(
        batchSize: Int = 200,
    ): Either<io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError, Unit> = processPending(batchSize)

    suspend fun processByIds(ids: List<String>): Either<ScopeManagementApplicationError, Unit> = either {
        val pending = outboxRepository.fetchPending(Int.MAX_VALUE).filter { it.id in ids.toSet() }
        if (pending.isEmpty()) return@either
        processRecords(pending.map { it.id }).bind()
    }

    private suspend fun processRecords(ids: List<String>): Either<ScopeManagementApplicationError, Unit> = either {
        val idSet = ids.toSet()
        val rows = outboxRepository.fetchPending(Int.MAX_VALUE).filter { it.id in idSet }
        for (row in rows) {
            try {
                val event = json.decodeFromString<DomainEvent>(row.payload)
                projectionService.projectEvent(event).bind()
                outboxRepository.markProcessed(row.id, Clock.System.now())
            } catch (e: Exception) {
                logger.error("Projection failed for outbox ${row.id}", mapOf("error" to e.message.orEmpty()))
                outboxRepository.markFailed(row.id)
                raise(
                    ScopeManagementApplicationError.PersistenceError.ProjectionFailed(
                        eventType = row.event_type,
                        aggregateId = row.aggregate_id,
                        reason = e.message ?: "(no_message)",
                    ),
                )
            }
        }
    }
}
