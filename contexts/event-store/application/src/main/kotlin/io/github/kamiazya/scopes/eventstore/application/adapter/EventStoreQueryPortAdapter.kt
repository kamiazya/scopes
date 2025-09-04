package io.github.kamiazya.scopes.eventstore.application.adapter

import arrow.core.Either
import arrow.core.flatMap
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreQueryPort
import io.github.kamiazya.scopes.contracts.eventstore.errors.EventStoreContractError
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsByAggregateQuery
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
