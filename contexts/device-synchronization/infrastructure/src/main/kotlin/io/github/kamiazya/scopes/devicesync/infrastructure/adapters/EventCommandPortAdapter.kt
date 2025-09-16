package io.github.kamiazya.scopes.devicesync.infrastructure.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreCommandPort
import io.github.kamiazya.scopes.contracts.eventstore.commands.StoreEventCommand
import io.github.kamiazya.scopes.devicesync.application.error.DeviceSyncApplicationError
import io.github.kamiazya.scopes.devicesync.application.port.EventCommandPort
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Infrastructure adapter that implements EventCommandPort using event store contracts.
 *
 * This adapter bridges the application layer's EventCommandPort with the
 * concrete event store contract implementation for writing events.
 */
class EventCommandPortAdapter(private val eventStoreCommandPort: EventStoreCommandPort, private val logger: Logger, private val json: Json) :
    EventCommandPort {

    override suspend fun append(event: DomainEvent): Either<DeviceSyncApplicationError, Unit> {
        logger.debug("Appending single event: ${event::class.simpleName}")

        return appendSingle(event)
    }

    override suspend fun appendBatch(events: List<DomainEvent>): Either<DeviceSyncApplicationError, Unit> {
        logger.debug("Appending batch of ${events.size} events")

        if (events.isEmpty()) {
            return Either.Right(Unit)
        }

        // For simplicity, append events one by one
        // In a real implementation, you might want to use a batch append command
        for (event in events) {
            when (val result = appendSingle(event)) {
                is Either.Left -> return result
                is Either.Right -> continue
            }
        }

        logger.debug("Successfully appended batch of ${events.size} events")
        return Either.Right(Unit)
    }

    private suspend fun appendSingle(event: DomainEvent): Either<DeviceSyncApplicationError, Unit> = try {
        val eventData = json.encodeToString(event)
        val eventType = event::class.simpleName ?: error("Event class name cannot be null")

        eventStoreCommandPort.createEvent(
            StoreEventCommand(
                aggregateId = event.aggregateId.value,
                aggregateVersion = event.aggregateVersion.value,
                eventType = eventType,
                eventData = eventData,
                occurredAt = event.occurredAt,
            ),
        ).mapLeft { contractError ->
            logger.error(
                "Failed to append event to event store",
                mapOf(
                    "aggregateId" to event.aggregateId.value,
                    "eventType" to eventType,
                    "eventId" to event.eventId.value,
                    "error" to contractError.toString(),
                ),
            )

            DeviceSyncApplicationError.EventStoreError(
                operation = DeviceSyncApplicationError.EventStoreOperation.APPEND,
                aggregateId = event.aggregateId.value,
                eventType = eventType,
                eventCount = 1,
            )
        }.map {
            logger.debug("Successfully appended event: $eventType")
            Unit
        }
    } catch (e: Exception) {
        val eventType = event::class.simpleName ?: error("Event class name cannot be null")
        logger.error(
            "Failed to serialize event for appending",
            mapOf(
                "aggregateId" to event.aggregateId.value,
                "eventType" to eventType,
                "eventId" to event.eventId.value,
                "error" to (e.message ?: "Unknown serialization error"),
            ),
        )

        Either.Left(
            DeviceSyncApplicationError.EventStoreError(
                operation = DeviceSyncApplicationError.EventStoreOperation.APPEND,
                aggregateId = event.aggregateId.value,
                eventType = eventType,
                eventCount = 1,
            ),
        )
    }
}
