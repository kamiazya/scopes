package io.github.kamiazya.scopes.eventstore.application.handler.query

import arrow.core.Either
import io.github.kamiazya.scopes.eventstore.application.dto.PersistedEventRecordDto
import io.github.kamiazya.scopes.eventstore.application.error.EventStoreApplicationError
import io.github.kamiazya.scopes.eventstore.application.query.GetEventsByAggregateQuery
import io.github.kamiazya.scopes.eventstore.domain.repository.EventRepository
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler

/**
 * Handler for retrieving events by aggregate ID.
 */
class GetEventsByAggregateHandler(private val eventRepository: EventRepository) :
    QueryHandler<GetEventsByAggregateQuery, EventStoreApplicationError, List<PersistedEventRecordDto>> {

    override suspend fun invoke(query: GetEventsByAggregateQuery): Either<EventStoreApplicationError, List<PersistedEventRecordDto>> {
        // Validate query
        query.limit?.let { limit ->
            if (limit <= 0) {
                return Either.Left(
                    EventStoreApplicationError.ValidationError(
                        parameter = "limit",
                        invalidValue = limit,
                        constraint = EventStoreApplicationError.ValidationConstraint.ZERO_LIMIT,
                    ),
                )
            }
        }

        return eventRepository.getEventsByAggregate(
            aggregateId = query.aggregateId,
            since = query.since,
            limit = query.limit,
        )
            .mapLeft { error ->
                EventStoreApplicationError.RepositoryError(
                    operation = EventStoreApplicationError.RepositoryOperation.GET_AGGREGATE_EVENTS,
                    aggregateId = query.aggregateId.value,
                    cause = null,
                )
            }
            .map { events ->
                events.map { storedEvent ->
                    PersistedEventRecordDto(
                        eventId = storedEvent.metadata.eventId.value,
                        aggregateId = storedEvent.metadata.aggregateId.value,
                        aggregateVersion = storedEvent.metadata.aggregateVersion.value,
                        eventType = storedEvent.metadata.eventType.value,
                        occurredAt = storedEvent.metadata.occurredAt,
                        storedAt = storedEvent.metadata.storedAt,
                        sequenceNumber = storedEvent.metadata.sequenceNumber,
                        event = storedEvent.event, // Include the domain event
                    )
                }
            }
    }
}
