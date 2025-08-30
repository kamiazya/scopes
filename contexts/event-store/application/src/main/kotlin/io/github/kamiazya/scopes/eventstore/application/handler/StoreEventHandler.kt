package io.github.kamiazya.scopes.eventstore.application.handler

import arrow.core.Either
import io.github.kamiazya.scopes.eventstore.application.command.StoreEvent
import io.github.kamiazya.scopes.eventstore.application.dto.PersistedEventRecordDto
import io.github.kamiazya.scopes.eventstore.application.error.EventStoreApplicationError
import io.github.kamiazya.scopes.eventstore.domain.repository.EventRepository
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.application.usecase.UseCase

/**
 * Handler for storing domain events in the event store.
 *
 * This handler implements the application layer use case for persisting events.
 * It ensures that all event storage operations are performed within a transaction
 * to maintain consistency and atomicity.
 *
 * ## Transaction Management
 * The handler uses [TransactionManager] to wrap the repository operation,
 * ensuring that event storage is atomic and can be rolled back on failure.
 *
 * ## Error Handling
 * Repository errors are mapped to application-specific errors with additional
 * context about the operation that failed.
 *
 * @property eventRepository The repository for persisting events
 * @property transactionManager The transaction manager for ensuring atomicity
 *
 * @since 1.0.0
 * @see StoreEvent
 * @see PersistedEventRecordDto
 */
class StoreEventHandler(private val eventRepository: EventRepository, private val transactionManager: TransactionManager) :
    UseCase<StoreEvent, EventStoreApplicationError, PersistedEventRecordDto> {

    /**
     * Stores a domain event in the event store.
     *
     * @param input The command containing the event to store
     * @return Either an error or the DTO representing the persisted event
     */
    override suspend fun invoke(input: StoreEvent): Either<EventStoreApplicationError, PersistedEventRecordDto> = transactionManager.inTransaction {
        eventRepository.store(input.event)
            .mapLeft { error ->
                EventStoreApplicationError.RepositoryError(
                    operation = EventStoreApplicationError.RepositoryOperation.APPEND_EVENT,
                    aggregateId = input.event.aggregateId.value,
                    eventType = input.event::class.simpleName
                        ?: input.event::class.qualifiedName
                        ?: "DomainEvent",
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
}
