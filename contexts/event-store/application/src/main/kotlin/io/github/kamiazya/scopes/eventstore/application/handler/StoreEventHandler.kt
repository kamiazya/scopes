package io.github.kamiazya.scopes.eventstore.application.handler

import arrow.core.Either
import io.github.kamiazya.scopes.eventstore.application.command.StoreEvent
import io.github.kamiazya.scopes.eventstore.application.dto.PersistedEventRecordDto
import io.github.kamiazya.scopes.eventstore.application.error.EventStoreApplicationError
import io.github.kamiazya.scopes.eventstore.domain.repository.EventRepository
import io.github.kamiazya.scopes.platform.application.usecase.UseCase

/**
 * Handler for storing events.
 */
class StoreEventHandler(private val eventRepository: EventRepository) : UseCase<StoreEvent, EventStoreApplicationError, PersistedEventRecordDto> {

    override suspend fun invoke(input: StoreEvent): Either<EventStoreApplicationError, PersistedEventRecordDto> = eventRepository.store(input.event)
        .mapLeft { error ->
            EventStoreApplicationError.RepositoryError(
                operation = EventStoreApplicationError.RepositoryOperation.APPEND_EVENT,
                aggregateId = input.event.aggregateId.value,
                eventType = input.event::class.simpleName ?: "UnknownEvent",
                occurredAt = error.occurredAt,
                cause = null,
            )
        }
        .map { storedEvent ->
            PersistedEventRecordDto(
                eventId = storedEvent.metadata.eventId.value,
                aggregateId = storedEvent.metadata.aggregateId.value,
                aggregateVersion = storedEvent.metadata.aggregateVersion.value,
                eventType = storedEvent.metadata.eventType.value,
                occurredAt = storedEvent.metadata.occurredAt,
                storedAt = storedEvent.metadata.storedAt,
                sequenceNumber = storedEvent.metadata.sequenceNumber,
            )
        }
}
