package io.github.kamiazya.scopes.collaborativeversioning.infrastructure.subscriber

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.collaborativeversioning.application.error.EventHandlingError
import io.github.kamiazya.scopes.collaborativeversioning.application.handler.DomainEventHandlerRegistry
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreQueryPort
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsSinceQuery
import io.github.kamiazya.scopes.eventstore.application.port.EventSerializer
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = ConsoleLogger("EventStoreSubscriber")

/**
 * Subscribes to events from the event store and dispatches them to handlers.
 *
 * This component polls the event store for new events and ensures they are
 * processed by the appropriate handlers in the collaborative versioning context.
 */
class EventStoreSubscriber(
    private val eventStoreQueryPort: EventStoreQueryPort,
    private val eventSerializer: EventSerializer,
    private val handlerRegistry: DomainEventHandlerRegistry,
    private val pollingInterval: Duration = 5.seconds,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) {
    private var pollingJob: Job? = null
    private var lastProcessedTimestamp: Instant? = null

    /**
     * Start subscribing to events.
     *
     * @param eventTypes List of event type IDs to subscribe to
     * @param fromTimestamp Start processing events from this timestamp
     */
    fun start(eventTypes: List<String>, fromTimestamp: Instant? = null) {
        logger.info("Starting event store subscriber for types: $eventTypes")

        lastProcessedTimestamp = fromTimestamp

        pollingJob = coroutineScope.launch {
            while (isActive) {
                try {
                    pollAndProcessEvents(eventTypes)
                } catch (e: Exception) {
                    logger.error("Error polling events", throwable = e)
                }

                delay(pollingInterval)
            }
        }
    }

    /**
     * Stop subscribing to events.
     */
    fun stop() {
        logger.info("Stopping event store subscriber")
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun pollAndProcessEvents(eventTypes: List<String>) {
        // For now, we'll poll all events since last processed timestamp
        // In a real system, you might want to filter by event types
        val eventsFlow = if (lastProcessedTimestamp != null) {
            val query = GetEventsSinceQuery(
                since = lastProcessedTimestamp!!,
                limit = 100,
            )
            eventStoreQueryPort.getEventsSince(query)
        } else {
            // If no last processed timestamp, start from the beginning
            // This would need proper implementation based on your needs
            val query = GetEventsSinceQuery(
                since = Instant.DISTANT_PAST,
                limit = 100,
            )
            eventStoreQueryPort.getEventsSince(query)
        }

        eventsFlow.fold(
            { error ->
                logger.error("Failed to query events: $error")
            },
            { events ->
                events.forEach { eventResult ->
                    processEvent(eventResult).fold(
                        { error ->
                            logger.error("Failed to process event ${eventResult.eventId}: $error")
                        },
                        {
                            lastProcessedTimestamp = eventResult.occurredAt
                        },
                    )
                }
            },
        )
    }

    private suspend fun processEvent(eventResult: io.github.kamiazya.scopes.contracts.eventstore.results.EventResult): Either<EventHandlingError, Unit> =
        either {
            // Deserialize the event
            val domainEvent = eventSerializer.deserialize(
                eventType = eventResult.eventType,
                eventData = eventResult.eventData,
            ).fold(
                { deserializationError ->
                    val eventIdOrError = io.github.kamiazya.scopes.platform.domain.value.EventId.from(eventResult.eventId)
                    val eventId = eventIdOrError.fold(
                        ifLeft = { io.github.kamiazya.scopes.platform.domain.value.EventId.generate() },
                        ifRight = { it },
                    )
                    raise(
                        EventHandlingError.InvalidEventData(
                            eventId = eventId,
                            eventType = eventResult.eventType,
                            details = "Failed to deserialize: $deserializationError",
                        ),
                    )
                },
                { it },
            )

            // Dispatch to handlers
            handlerRegistry.dispatch(domainEvent).bind()

            logger.debug(
                "Successfully processed event ${eventResult.eventId} of type ${eventResult.eventType}",
            )
        }
}
