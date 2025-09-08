package io.github.kamiazya.scopes.contracts.eventstore

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.eventstore.commands.StoreEventCommand
import io.github.kamiazya.scopes.contracts.eventstore.errors.EventStoreContractError

/**
 * Public contract for event store write operations (Commands).
 * Following CQRS principles, this port handles only operations that modify state.
 * All operations return Either for explicit error handling.
 */
public interface EventStoreCommandPort {
    /**
     * Stores a new event in the event store.
     * Returns minimal success indicator following CQRS principles.
     * @param command The command containing event details to store
     * @return Either an error or unit indicating successful storage
     */
    public suspend fun createEvent(command: StoreEventCommand): Either<EventStoreContractError, Unit>
}
