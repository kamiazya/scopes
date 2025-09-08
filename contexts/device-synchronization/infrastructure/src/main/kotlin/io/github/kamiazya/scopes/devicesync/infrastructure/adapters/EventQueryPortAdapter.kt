package io.github.kamiazya.scopes.devicesync.infrastructure.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreQueryPort
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsSinceQuery
import io.github.kamiazya.scopes.contracts.eventstore.results.EventResult
import io.github.kamiazya.scopes.devicesync.application.error.DeviceSyncApplicationError
import io.github.kamiazya.scopes.devicesync.application.port.EventQueryPort
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

/**
 * Infrastructure adapter that implements EventQueryPort using event store contracts.
 *
 * This adapter bridges the application layer's EventQueryPort with the
 * concrete event store contract implementation for reading events.
 */
class EventQueryPortAdapter(private val eventStoreQueryPort: EventStoreQueryPort, private val logger: Logger, private val json: Json) : EventQueryPort {

    override suspend fun getEventsSince(since: Instant, limit: Int): Either<DeviceSyncApplicationError, List<DomainEvent>> {
        logger.debug("Retrieving events since $since with limit $limit")

        return eventStoreQueryPort.getEventsSince(
            GetEventsSinceQuery(
                since = since,
                limit = limit,
            ),
        ).mapLeft { contractError ->
            logger.error(
                "Failed to retrieve events from event store",
                mapOf(
                    "since" to since.toString(),
                    "limit" to limit.toString(),
                    "error" to contractError.toString(),
                ),
            )

            DeviceSyncApplicationError.EventStoreError(
                operation = DeviceSyncApplicationError.EventStoreOperation.QUERY_EVENTS,
                aggregateId = "all", // Query operations don't have specific aggregate
                eventType = null,
                eventCount = limit,
                cause = contractError as? Throwable,
            )
        }.map { eventResults ->
            logger.debug("Successfully retrieved ${eventResults.size} events from event store")
            eventResults.mapNotNull { result -> deserializeEvent(result) }
        }
    }

    /**
     * Deserializes an EventResult back to a DomainEvent.
     * Returns null if deserialization fails (event type unknown or data corrupted).
     */
    private fun deserializeEvent(result: EventResult): DomainEvent? = try {
        // In a real implementation, you would use a registry or reflection
        // to map event types to their classes. For now, we'll deserialize
        // and return the event as-is since it already contains all the fields
        json.decodeFromString<DomainEvent>(result.eventData)
    } catch (e: Exception) {
        logger.warn(
            "Failed to deserialize event",
            mapOf(
                "eventId" to result.eventId,
                "eventType" to result.eventType,
                "error" to (e.message ?: "Unknown error"),
            ),
        )
        null
    }
}
