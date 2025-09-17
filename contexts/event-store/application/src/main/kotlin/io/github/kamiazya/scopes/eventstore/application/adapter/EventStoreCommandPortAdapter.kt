package io.github.kamiazya.scopes.eventstore.application.adapter

import arrow.core.Either
import arrow.core.flatMap
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreCommandPort
import io.github.kamiazya.scopes.contracts.eventstore.commands.StoreEventCommand
import io.github.kamiazya.scopes.contracts.eventstore.errors.EventStoreContractError
import io.github.kamiazya.scopes.eventstore.application.handler.command.StoreEventHandler
import io.github.kamiazya.scopes.eventstore.application.port.EventSerializer
import io.github.kamiazya.scopes.eventstore.application.command.StoreEventCommand as AppStoreEventCommand

/**
 * Adapter that implements the Event Store command contract port.
 * This allows external bounded contexts to store events in the Event Store.
 */
class EventStoreCommandPortAdapter(private val storeEventHandler: StoreEventHandler, private val eventSerializer: EventSerializer) : EventStoreCommandPort {

    override suspend fun createEvent(command: StoreEventCommand): Either<EventStoreContractError, Unit> {
        // Deserialize the event data
        return eventSerializer.deserialize(command.eventType, command.eventData)
            .mapLeft { _ ->
                EventStoreContractError.EventStorageError(
                    aggregateId = command.aggregateId,
                    eventType = command.eventType,
                    storageReason = EventStoreContractError.StorageFailureReason.INVALID_EVENT,
                    cause = null,
                )
            }
            .flatMap { domainEvent ->
                // Store the event
                storeEventHandler(
                    AppStoreEventCommand(event = domainEvent),
                )
                    .mapLeft { _ ->
                        EventStoreContractError.EventStorageError(
                            aggregateId = command.aggregateId,
                            eventType = command.eventType,
                            storageReason = EventStoreContractError.StorageFailureReason.WRITE_TIMEOUT,
                            cause = null,
                        )
                    }
                    .map { Unit }
            }
    }
}
