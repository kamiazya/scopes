package io.github.kamiazya.scopes.collaborativeversioning.infrastructure.subscriber

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.collaborativeversioning.application.error.EventHandlingError
import io.github.kamiazya.scopes.collaborativeversioning.application.handler.DomainEventHandlerRegistry
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreQueryPort
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsByTypeQuery
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsSinceQuery
import io.github.kamiazya.scopes.eventstore.application.port.EventSerializer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import mu.KotlinLogging
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

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
        logger.info { "Starting event store subscriber for types: $eventTypes" }

        lastProcessedTimestamp = fromTimestamp

        pollingJob = coroutineScope.launch {
            while (isActive) {
                try {
                    pollAndProcessEvents(eventTypes)
                } catch (e: Exception) {
                    logger.error(e) { "Error polling events" }
                }

                delay(pollingInterval)
            }
        }
    }

    /**
     * Stop subscribing to events.
     */
    fun stop() {
        logger.info { "Stopping event store subscriber" }
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun pollAndProcessEvents(eventTypes: List<String>) {
        val query = lastProcessedTimestamp?.let {
            GetEventsSinceQuery(
                since = it,
                eventTypes = eventTypes,
                limit = 100,
            )
        } ?: GetEventsByTypeQuery(
            eventTypes = eventTypes,
            limit = 100,
        )

        eventStoreQueryPort.getEventsSince(query)
            .onRight { events ->
                events.collect { eventResult ->
                    processEvent(eventResult)
                        .onRight {
                            lastProcessedTimestamp = eventResult.occurredAt
                        }
                        .onLeft { error ->
                            logger.error { "Failed to process event ${eventResult.eventId}: $error" }
                        }
                }
            }
            .onLeft { error ->
                logger.error { "Failed to query events: $error" }
            }
    }

    private suspend fun processEvent(eventResult: io.github.kamiazya.scopes.contracts.eventstore.results.EventResult): Either<EventHandlingError, Unit> =
        either {
            // Deserialize the event
            val domainEvent = eventSerializer.deserialize(
                eventType = eventResult.eventType,
                eventData = eventResult.eventData,
            ).mapLeft { deserializationError ->
                EventHandlingError.InvalidEventData(
                    eventId = io.github.kamiazya.scopes.platform.domain.value.EventId.from(eventResult.eventId),
                    eventType = eventResult.eventType,
                    details = "Failed to deserialize: ${deserializationError.message}",
                )
            }.bind()

            // Dispatch to handlers
            handlerRegistry.dispatch(domainEvent).bind()

            logger.debug {
                "Successfully processed event ${eventResult.eventId} of type ${eventResult.eventType}"
            }
        }
}
