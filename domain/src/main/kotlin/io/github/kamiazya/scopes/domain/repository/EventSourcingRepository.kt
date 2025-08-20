package io.github.kamiazya.scopes.domain.repository

import arrow.core.Either
import io.github.kamiazya.scopes.domain.error.ScopesError
import io.github.kamiazya.scopes.domain.event.DomainEvent
import io.github.kamiazya.scopes.domain.valueobject.AggregateId

/**
 * Repository interface for event sourcing operations.
 *
 * This interface defines the contract for persisting and retrieving domain events,
 * which form the foundation of the event-sourced system. Events are the source of
 * truth and aggregates can be reconstructed by replaying their event history.
 *
 * Design principles:
 * - Events are immutable once stored
 * - Events must be stored in chronological order
 * - Optimistic concurrency control via version checking
 * - Support for event streaming and snapshots (future enhancement)
 *
 * Implementation notes:
 * - Implementations should ensure ACID properties for event storage
 * - Consider using event store databases like EventStore or Kafka
 * - For simpler implementations, RDBMS with proper constraints works well
 */
interface EventSourcingRepository<T> {

    /**
     * Saves a list of events for an aggregate.
     *
     * This operation should be atomic - either all events are saved or none.
     * The implementation must validate that events are being appended in the
     * correct version sequence to prevent concurrent modification issues.
     *
     * @param aggregateId The ID of the aggregate these events belong to
     * @param events The list of events to persist
     * @param expectedVersion The expected current version of the aggregate (for optimistic locking)
     * @return Either an error or Unit on successful save
     */
    suspend fun saveEvents(
        aggregateId: AggregateId,
        events: List<DomainEvent>,
        expectedVersion: Int,
    ): Either<ScopesError, Unit>

    /**
     * Retrieves all events for an aggregate in chronological order.
     *
     * Events should be returned in the order they were applied (by version).
     * This is used to reconstruct the aggregate's current state.
     *
     * @param aggregateId The ID of the aggregate to retrieve events for
     * @return Either an error or the list of events (empty if aggregate doesn't exist)
     */
    suspend fun getEvents(aggregateId: AggregateId): Either<ScopesError, List<DomainEvent>>

    /**
     * Retrieves events for an aggregate starting from a specific version.
     *
     * This is useful for:
     * - Loading events after a snapshot
     * - Implementing event replay from a specific point
     * - Debugging and auditing
     *
     * @param aggregateId The ID of the aggregate
     * @param fromVersion The version to start from (inclusive)
     * @return Either an error or the list of events
     */
    suspend fun getEventsFromVersion(aggregateId: AggregateId, fromVersion: Int): Either<ScopesError, List<DomainEvent>>

    /**
     * Retrieves events for an aggregate within a version range.
     *
     * Useful for partial replay and debugging specific state transitions.
     *
     * @param aggregateId The ID of the aggregate
     * @param fromVersion The starting version (inclusive)
     * @param toVersion The ending version (inclusive)
     * @return Either an error or the list of events
     */
    suspend fun getEventsBetweenVersions(
        aggregateId: AggregateId,
        fromVersion: Int,
        toVersion: Int,
    ): Either<ScopesError, List<DomainEvent>>

    /**
     * Gets the current version of an aggregate.
     *
     * This is the version of the last event applied to the aggregate.
     * Returns 0 if the aggregate doesn't exist.
     *
     * @param aggregateId The ID of the aggregate
     * @return Either an error or the current version (0 if not found)
     */
    suspend fun getCurrentVersion(aggregateId: AggregateId): Either<ScopesError, Int>

    /**
     * Checks if an aggregate exists.
     *
     * An aggregate exists if it has at least one event.
     *
     * @param aggregateId The ID of the aggregate to check
     * @return Either an error or true if exists, false otherwise
     */
    suspend fun exists(aggregateId: AggregateId): Either<ScopesError, Boolean>

    /**
     * Gets all events of a specific type across all aggregates.
     *
     * This is useful for:
     * - Building read models/projections
     * - Analytics and reporting
     * - Event replay for specific event types
     *
     * @param eventType The type of events to retrieve (e.g., "ScopeCreated")
     * @param limit Maximum number of events to return
     * @param offset Number of events to skip (for pagination)
     * @return Either an error or the list of events
     */
    suspend fun getEventsByType(
        eventType: String,
        limit: Int = 100,
        offset: Int = 0,
    ): Either<ScopesError, List<DomainEvent>>

    /**
     * Gets all events within a time range across all aggregates.
     *
     * Useful for:
     * - Time-based analytics
     * - Audit trails
     * - Debugging issues that occurred at specific times
     *
     * @param from Start time (inclusive)
     * @param to End time (exclusive)
     * @param limit Maximum number of events to return
     * @param offset Number of events to skip (for pagination)
     * @return Either an error or the list of events
     */
    suspend fun getEventsByTimeRange(
        from: kotlinx.datetime.Instant,
        to: kotlinx.datetime.Instant,
        limit: Int = 100,
        offset: Int = 0,
    ): Either<ScopesError, List<DomainEvent>>

    /**
     * Saves a snapshot of an aggregate's state.
     *
     * Snapshots are performance optimizations that allow faster aggregate
     * reconstruction by storing the state at a specific version.
     *
     * @param aggregateId The ID of the aggregate
     * @param snapshot The serialized snapshot data
     * @param version The version this snapshot represents
     * @return Either an error or Unit on successful save
     */
    suspend fun saveSnapshot(aggregateId: AggregateId, snapshot: ByteArray, version: Int): Either<ScopesError, Unit>

    /**
     * Retrieves the latest snapshot for an aggregate.
     *
     * Returns the most recent snapshot if one exists, along with its version.
     * Events from version + 1 onwards need to be applied to reconstruct current state.
     *
     * @param aggregateId The ID of the aggregate
     * @return Either an error or a pair of (snapshot data, version), or null if no snapshot exists
     */
    suspend fun getLatestSnapshot(aggregateId: AggregateId): Either<ScopesError, Pair<ByteArray, Int>?>
}
