package io.github.kamiazya.scopes.contracts.eventstore

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.eventstore.commands.StoreEventCommand
import io.github.kamiazya.scopes.contracts.eventstore.errors.EventStoreContractError
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsByAggregateQuery
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsSinceQuery
import io.github.kamiazya.scopes.contracts.eventstore.results.EventResult

/**
 * Public contract for event store operations.
 * Provides basic event persistence and retrieval capabilities.
 */
public interface EventStorePort {
    /**
     * Stores a new event.
     * @param command The command containing event details
     * @return Either an error or the stored event result
     */
    public suspend fun createEvent(command: StoreEventCommand): Either<EventStoreContractError, EventResult>

    /**
     * Retrieves events by aggregate ID.
     * @param query The query containing aggregate ID and optional filters
     * @return Either an error or list of events for the aggregate
     */
    public suspend fun getEventsByAggregate(query: GetEventsByAggregateQuery): Either<EventStoreContractError, List<EventResult>>

    /**
     * Retrieves events since a specific timestamp.
     * @param query The query containing the timestamp filter
     * @return Either an error or list of events since the timestamp
     */
    public suspend fun getEventsSince(query: GetEventsSinceQuery): Either<EventStoreContractError, List<EventResult>>
}
