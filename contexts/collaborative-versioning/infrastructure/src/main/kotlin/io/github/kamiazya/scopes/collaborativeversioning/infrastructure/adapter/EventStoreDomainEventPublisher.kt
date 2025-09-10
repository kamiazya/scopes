package io.github.kamiazya.scopes.collaborativeversioning.infrastructure.adapter

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.collaborativeversioning.application.error.EventPublishingError
import io.github.kamiazya.scopes.collaborativeversioning.application.port.DomainEventPublisher
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreCommandPort
import io.github.kamiazya.scopes.contracts.eventstore.commands.StoreEventCommand
import io.github.kamiazya.scopes.eventstore.application.port.EventSerializer
import io.github.kamiazya.scopes.eventstore.domain.model.EventTypeMapping
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Implementation of DomainEventPublisher that integrates with the event store.
 *
 * This adapter bridges the collaborative versioning context with the event store
 * context, translating domain events into event store commands.
 */
class EventStoreDomainEventPublisher(
    private val eventStoreCommandPort: EventStoreCommandPort,
    private val eventSerializer: EventSerializer,
    private val eventTypeMapping: EventTypeMapping,
    private val publishTimeout: Duration = 30.seconds,
) : DomainEventPublisher {

    override suspend fun publish(event: DomainEvent): Either<EventPublishingError, Unit> = either {
        // Validate event type is registered
        val typeId = validateEventType(event).bind()

        // Serialize the event
        val serializedEvent = eventSerializer.serialize(event)
            .mapLeft { serializationError ->
                EventPublishingError.SerializationFailed(
                    eventId = event.eventId,
                    eventType = typeId,
                    reason = serializationError.message,
                )
            }
            .bind()

        // Create store command
        val storeCommand = StoreEventCommand(
            aggregateId = event.aggregateId.toString(),
            aggregateVersion = event.aggregateVersion.value,
            eventType = typeId,
            eventData = serializedEvent,
            occurredAt = event.occurredAt,
            metadata = buildEventMetadata(event),
        )

        // Store event with timeout
        try {
            withTimeout(publishTimeout) {
                eventStoreCommandPort.createEvent(storeCommand)
                    .mapLeft { contractError ->
                        EventPublishingError.StorageFailed(
                            eventId = event.eventId,
                            eventType = typeId,
                            reason = "Event store error: ${contractError.message}",
                        )
                    }
                    .bind()
            }
        } catch (e: Exception) {
            raise(
                EventPublishingError.PublishTimeout(
                    eventId = event.eventId,
                    eventType = typeId,
                    timeoutMs = publishTimeout.inWholeMilliseconds,
                ),
            )
        }
    }

    override suspend fun publishAll(events: List<DomainEvent>): Either<EventPublishingError, Unit> = either {
        events.forEach { event ->
            publish(event).bind()
        }
    }

    private fun validateEventType(event: DomainEvent): Either<EventPublishingError, String> = either {
        val eventClass = event::class
        val typeId = try {
            eventTypeMapping.getTypeId(eventClass)
        } catch (e: Exception) {
            null
        }

        ensureNotNull(typeId) {
            EventPublishingError.UnregisteredEventType(
                eventType = eventClass.simpleName ?: "Unknown",
                eventClass = eventClass.qualifiedName ?: "Unknown",
            )
        }

        typeId
    }

    private fun buildEventMetadata(event: DomainEvent): Map<String, String> {
        val metadata = mutableMapOf<String, String>()

        // Add event ID as metadata
        metadata["eventId"] = event.eventId.toString()

        // Add metadata from event if present
        event.metadata?.let { meta ->
            meta.userId?.let { metadata["userId"] = it }
            meta.correlationId?.let { metadata["correlationId"] = it }
            meta.causationId?.let { metadata["causationId"] = it.toString() }
            metadata.putAll(meta.custom)
        }

        return metadata
    }
}
