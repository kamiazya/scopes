package io.github.kamiazya.scopes.eventstore.application.handler

import arrow.core.Either
import io.github.kamiazya.scopes.eventstore.application.dto.PersistedEventRecordDto
import io.github.kamiazya.scopes.eventstore.application.error.EventStoreApplicationError
import io.github.kamiazya.scopes.eventstore.application.query.GetEventsByAggregate
import io.github.kamiazya.scopes.eventstore.domain.repository.EventRepository
import io.github.kamiazya.scopes.platform.application.usecase.UseCase

/**
 * Handler for retrieving events by aggregate ID.
 */
class GetEventsByAggregateHandler(private val eventRepository: EventRepository) :
    UseCase<GetEventsByAggregate, EventStoreApplicationError, List<PersistedEventRecordDto>> {

    override suspend fun invoke(input: GetEventsByAggregate): Either<EventStoreApplicationError, List<PersistedEventRecordDto>> {
        // Validate input
        input.limit?.let { limit ->
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
            aggregateId = input.aggregateId,
            since = input.since,
            limit = input.limit,
        )
            .mapLeft { error ->
                EventStoreApplicationError.RepositoryError(
                    operation = EventStoreApplicationError.RepositoryOperation.GET_AGGREGATE_EVENTS,
                    aggregateId = input.aggregateId.value,
                    occurredAt = error.occurredAt,
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
