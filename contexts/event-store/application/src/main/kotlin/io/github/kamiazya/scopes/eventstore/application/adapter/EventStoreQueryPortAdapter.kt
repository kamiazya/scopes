package io.github.kamiazya.scopes.eventstore.application.adapter

import arrow.core.Either
import arrow.core.flatMap
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreQueryPort
import io.github.kamiazya.scopes.contracts.eventstore.errors.EventStoreContractError
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsByAggregateQuery
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsByTimeRangeQuery
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsByTypeQuery
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsSinceQuery
import io.github.kamiazya.scopes.contracts.eventstore.results.EventResult
import io.github.kamiazya.scopes.eventstore.application.handler.query.GetEventsByAggregateHandler
import io.github.kamiazya.scopes.eventstore.application.handler.query.GetEventsSinceHandler
import io.github.kamiazya.scopes.eventstore.application.port.EventSerializer
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.eventstore.application.query.GetEventsByAggregateQuery as AppGetEventsByAggregateQuery
import io.github.kamiazya.scopes.eventstore.application.query.GetEventsSinceQuery as AppGetEventsSinceQuery

/**
 * Adapter that implements the Event Store query contract port.
 * This allows external bounded contexts to retrieve events from the Event Store.
 */
class EventStoreQueryPortAdapter(
    private val getEventsByAggregateHandler: GetEventsByAggregateHandler,
    private val getEventsSinceHandler: GetEventsSinceHandler,
    private val eventSerializer: EventSerializer,
    private val eventRepository: io.github.kamiazya.scopes.eventstore.domain.repository.EventRepository,
) : EventStoreQueryPort {

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
                    AppGetEventsByAggregateQuery(
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
                                    occurredAt = kotlinx.datetime.Clock.System.now(),
                                )
                            else ->
                                EventStoreContractError.EventRetrievalError(
                                    aggregateId = query.aggregateId,
                                    retrievalReason = EventStoreContractError.RetrievalFailureReason.TIMEOUT,
                                    occurredAt = kotlinx.datetime.Clock.System.now(),
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
        AppGetEventsSinceQuery(
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
                        occurredAt = kotlinx.datetime.Clock.System.now(),
                    )
                else ->
                    EventStoreContractError.EventRetrievalError(
                        retrievalReason = EventStoreContractError.RetrievalFailureReason.TIMEOUT,
                        occurredAt = kotlinx.datetime.Clock.System.now(),
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

    override suspend fun getEventsByType(query: GetEventsByTypeQuery): Either<EventStoreContractError, List<EventResult>> {
        // Use the event repository to query by event type
        return try {
            val events = eventRepository.findByEventType(
                eventType = query.eventType,
                limit = query.limit,
                offset = query.offset,
            )

            val results = events.mapNotNull { storedEvent ->
                when (val serialized = eventSerializer.serialize(storedEvent.event)) {
                    is Either.Right -> EventResult(
                        eventId = storedEvent.metadata.eventId.value,
                        aggregateId = storedEvent.metadata.aggregateId.value,
                        aggregateVersion = storedEvent.metadata.aggregateVersion.value,
                        eventType = storedEvent.metadata.eventType.value,
                        eventData = serialized.value,
                        occurredAt = storedEvent.metadata.occurredAt,
                        storedAt = storedEvent.metadata.storedAt,
                        sequenceNumber = storedEvent.metadata.sequenceNumber,
                    )
                    is Either.Left -> null // Skip events that can't be serialized
                }
            }
            Either.Right(results)
        } catch (e: Exception) {
            Either.Left(
                EventStoreContractError.EventRetrievalError(
                    retrievalReason = EventStoreContractError.RetrievalFailureReason.CORRUPTED_DATA,
                    occurredAt = kotlinx.datetime.Clock.System.now(),
                    cause = e,
                ),
            )
        }
    }

    override suspend fun getEventsByTimeRange(query: GetEventsByTimeRangeQuery): Either<EventStoreContractError, List<EventResult>> {
        // Use the event repository to query by time range
        return try {
            val events = eventRepository.findByTimeRange(
                from = query.from,
                to = query.to,
                limit = query.limit,
                offset = query.offset,
            )

            val results = events.mapNotNull { storedEvent ->
                when (val serialized = eventSerializer.serialize(storedEvent.event)) {
                    is Either.Right -> EventResult(
                        eventId = storedEvent.metadata.eventId.value,
                        aggregateId = storedEvent.metadata.aggregateId.value,
                        aggregateVersion = storedEvent.metadata.aggregateVersion.value,
                        eventType = storedEvent.metadata.eventType.value,
                        eventData = serialized.value,
                        occurredAt = storedEvent.metadata.occurredAt,
                        storedAt = storedEvent.metadata.storedAt,
                        sequenceNumber = storedEvent.metadata.sequenceNumber,
                    )
                    is Either.Left -> null // Skip events that can't be serialized
                }
            }
            Either.Right(results)
        } catch (e: Exception) {
            Either.Left(
                EventStoreContractError.EventRetrievalError(
                    retrievalReason = EventStoreContractError.RetrievalFailureReason.CORRUPTED_DATA,
                    occurredAt = kotlinx.datetime.Clock.System.now(),
                    cause = e,
                ),
            )
        }
    }
}
