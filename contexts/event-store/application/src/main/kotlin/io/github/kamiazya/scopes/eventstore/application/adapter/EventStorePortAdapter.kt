package io.github.kamiazya.scopes.eventstore.application.adapter

import arrow.core.Either
import arrow.core.flatMap
import io.github.kamiazya.scopes.contracts.eventstore.EventStorePort
import io.github.kamiazya.scopes.contracts.eventstore.commands.StoreEventCommand
import io.github.kamiazya.scopes.contracts.eventstore.errors.EventStoreContractError
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsByAggregateQuery
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsSinceQuery
import io.github.kamiazya.scopes.contracts.eventstore.results.EventResult
import io.github.kamiazya.scopes.eventstore.application.command.StoreEvent
import io.github.kamiazya.scopes.eventstore.application.handler.command.StoreEventHandler
import io.github.kamiazya.scopes.eventstore.application.handler.query.GetEventsByAggregateHandler
import io.github.kamiazya.scopes.eventstore.application.handler.query.GetEventsSinceHandler
import io.github.kamiazya.scopes.eventstore.application.port.EventSerializer
import io.github.kamiazya.scopes.eventstore.application.query.GetEventsByAggregate
import io.github.kamiazya.scopes.eventstore.application.query.GetEventsSince
import io.github.kamiazya.scopes.platform.domain.value.AggregateId

/**
 * Adapter that implements the Event Store contract port.
 * This allows external bounded contexts to interact with the Event Store.
 */
class EventStorePortAdapter(
    private val storeEventHandler: StoreEventHandler,
    private val getEventsByAggregateHandler: GetEventsByAggregateHandler,
    private val getEventsSinceHandler: GetEventsSinceHandler,
    private val eventSerializer: EventSerializer,
) : EventStorePort {

    override suspend fun createEvent(command: StoreEventCommand): Either<EventStoreContractError, EventResult> {
        // Deserialize the event data
        return eventSerializer.deserialize(command.eventType, command.eventData)
            .mapLeft { error ->
                EventStoreContractError.EventStorageError(
                    aggregateId = command.aggregateId,
                    eventType = command.eventType,
                    storageReason = EventStoreContractError.StorageFailureReason.INVALID_EVENT,
                    occurredAt = error.occurredAt,
                    cause = null,
                )
            }
            .flatMap { domainEvent ->
                // Store the event
                storeEventHandler(
                    StoreEvent(event = domainEvent),
                )
                    .mapLeft { error ->
                        EventStoreContractError.EventStorageError(
                            aggregateId = command.aggregateId,
                            eventType = command.eventType,
                            storageReason = EventStoreContractError.StorageFailureReason.WRITE_TIMEOUT,
                            occurredAt = error.occurredAt,
                            cause = null,
                        )
                    }
                    .map { storedEventDto ->
                        EventResult(
                            eventId = storedEventDto.eventId,
                            aggregateId = storedEventDto.aggregateId,
                            aggregateVersion = storedEventDto.aggregateVersion,
                            eventType = storedEventDto.eventType,
                            eventData = command.eventData, // Return the original eventData
                            occurredAt = storedEventDto.occurredAt,
                            storedAt = storedEventDto.storedAt,
                            sequenceNumber = storedEventDto.sequenceNumber,
                        )
                    }
            }
    }

    override suspend fun getEventsByAggregate(query: GetEventsByAggregateQuery): Either<EventStoreContractError, List<EventResult>> =
        AggregateId.from(query.aggregateId)
            .mapLeft { error ->
                EventStoreContractError.InvalidQueryError(
                    parameterName = "aggregateId",
                    providedValue = query.aggregateId,
                    constraint = EventStoreContractError.QueryConstraint.INVALID_FORMAT,
                    expectedFormat = "UUID format",
                    occurredAt = kotlinx.datetime.Clock.System.now(),
                )
            }
            .flatMap { aggregateId ->
                getEventsByAggregateHandler(
                    GetEventsByAggregate(
                        aggregateId = aggregateId,
                        since = query.since,
                        limit = query.limit,
                    ),
                )
                    .mapLeft { error ->
                        when (error) {
                            is io.github.kamiazya.scopes.eventstore.application.error.EventStoreApplicationError.ValidationError ->
                                EventStoreContractError.InvalidQueryError(
                                    parameterName = "query",
                                    providedValue = query,
                                    constraint = EventStoreContractError.QueryConstraint.INVALID_COMBINATION,
                                    occurredAt = error.occurredAt,
                                )
                            else ->
                                EventStoreContractError.EventRetrievalError(
                                    aggregateId = query.aggregateId,
                                    retrievalReason = EventStoreContractError.RetrievalFailureReason.TIMEOUT,
                                    occurredAt = error.occurredAt,
                                    cause = null,
                                )
                        }
                    }
                    .flatMap { storedEvents ->
                        // Serialize each event to get eventData
                        val results = storedEvents.mapNotNull { dto ->
                            dto.event?.let { event ->
                                when (val serialized = eventSerializer.serialize(event)) {
                                    is Either.Right -> EventResult(
                                        eventId = dto.eventId,
                                        aggregateId = dto.aggregateId,
                                        aggregateVersion = dto.aggregateVersion,
                                        eventType = dto.eventType,
                                        eventData = serialized.value,
                                        occurredAt = dto.occurredAt,
                                        storedAt = dto.storedAt,
                                        sequenceNumber = dto.sequenceNumber,
                                    )
                                    is Either.Left -> null // Skip events that can't be serialized
                                }
                            }
                        }
                        Either.Right(results)
                    }
            }

    override suspend fun getEventsSince(query: GetEventsSinceQuery): Either<EventStoreContractError, List<EventResult>> = getEventsSinceHandler(
        GetEventsSince(
            since = query.since,
            limit = query.limit,
        ),
    )
        .mapLeft { error ->
            when (error) {
                is io.github.kamiazya.scopes.eventstore.application.error.EventStoreApplicationError.ValidationError ->
                    EventStoreContractError.InvalidQueryError(
                        parameterName = "query",
                        providedValue = query,
                        constraint = EventStoreContractError.QueryConstraint.INVALID_FORMAT,
                        occurredAt = error.occurredAt,
                    )
                else ->
                    EventStoreContractError.EventRetrievalError(
                        retrievalReason = EventStoreContractError.RetrievalFailureReason.TIMEOUT,
                        occurredAt = error.occurredAt,
                        cause = null,
                    )
            }
        }
        .flatMap { storedEvents ->
            val results = storedEvents.mapNotNull { dto ->
                dto.event?.let { event ->
                    when (val serialized = eventSerializer.serialize(event)) {
                        is Either.Right -> EventResult(
                            eventId = dto.eventId,
                            aggregateId = dto.aggregateId,
                            aggregateVersion = dto.aggregateVersion,
                            eventType = dto.eventType,
                            eventData = serialized.value,
                            occurredAt = dto.occurredAt,
                            storedAt = dto.storedAt,
                            sequenceNumber = dto.sequenceNumber,
                        )
                        is Either.Left -> null // Skip events that can't be serialized
                    }
                }
            }
            Either.Right(results)
        }
}
