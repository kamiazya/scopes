package io.github.kamiazya.scopes.contracts.eventstore

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.eventstore.errors.EventStoreContractError
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsByAggregateQuery
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsByTimeRangeQuery
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsByTypeQuery
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsSinceQuery
import io.github.kamiazya.scopes.contracts.eventstore.results.EventResult

/**
 * Public contract for event store read operations (Queries).
 * Following CQRS principles, this port handles only operations that read data without side effects.
 * All operations return Either for explicit error handling.
 */
public interface EventStoreQueryPort {
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

    /**
     * Retrieves events by event type.
     * @param query The query containing event type and pagination parameters
     * @return Either an error or list of events of the specified type
     */
    public suspend fun getEventsByType(query: GetEventsByTypeQuery): Either<EventStoreContractError, List<EventResult>>

    /**
     * Retrieves events within a time range.
     * @param query The query containing time range and pagination parameters
     * @return Either an error or list of events within the time range
     */
    public suspend fun getEventsByTimeRange(query: GetEventsByTimeRangeQuery): Either<EventStoreContractError, List<EventResult>>
}
