package io.github.kamiazya.scopes.eventstore.domain.repository

import arrow.core.Either
import io.github.kamiazya.scopes.eventstore.domain.entity.PersistedEventRecord
import io.github.kamiazya.scopes.eventstore.domain.error.EventStoreError
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Repository interface for event storage and retrieval.
 *
 * This interface provides pure event persistence capabilities
 * without device synchronization concerns.
 */
interface EventRepository {
    /**
     * Stores a domain event.
     *
     * @param event The domain event to store
     * @return Either an error or the stored event with its metadata
     */
    suspend fun store(event: DomainEvent): Either<EventStoreError, PersistedEventRecord>

    /**
     * Retrieves events since a specific timestamp.
     *
     * @param since The timestamp to retrieve events after (exclusive)
     * @param limit Optional limit on number of events to retrieve
     * @return Either an error or a list of stored events
     */
    suspend fun getEventsSince(since: Instant, limit: Int? = null): Either<EventStoreError, List<PersistedEventRecord>>

    /**
     * Retrieves events by aggregate ID.
     *
     * @param aggregateId The aggregate ID to filter events by
     * @param since Optional timestamp to retrieve events after
     * @param limit Optional limit on number of events to retrieve
     * @return Either an error or a list of stored events
     */
    suspend fun getEventsByAggregate(aggregateId: AggregateId, since: Instant? = null, limit: Int? = null): Either<EventStoreError, List<PersistedEventRecord>>

    /**
     * Retrieves events by event type.
     *
     * @param eventType The event type to filter by
     * @param limit Optional limit on number of events to retrieve
     * @return Either an error or a list of stored events
     */
    suspend fun getEventsByType(eventType: String, limit: Int? = null): Either<EventStoreError, List<PersistedEventRecord>>

    /**
     * Retrieves events by event type since a specific timestamp.
     *
     * @param eventType The event type to filter by
     * @param since The timestamp to retrieve events after (based on occurredAt)
     * @param limit Optional limit on number of events to retrieve
     * @return Either an error or a list of stored events
     */
    suspend fun getEventsByTypeSince(eventType: String, since: Instant, limit: Int? = null): Either<EventStoreError, List<PersistedEventRecord>>

    /**
     * Retrieves events within a time range.
     *
     * @param from The start timestamp (inclusive, based on occurredAt)
     * @param to The end timestamp (exclusive, based on occurredAt)
     * @param limit Optional limit on number of events to retrieve
     * @return Either an error or a list of stored events
     */
    suspend fun getEventsByTimeRange(from: Instant, to: Instant, limit: Int? = null): Either<EventStoreError, List<PersistedEventRecord>>

    /**
     * Streams all events as they are stored.
     *
     * @return A Flow of stored events
     */
    fun streamEvents(): Flow<PersistedEventRecord>

    /**
     * Finds events by event type with pagination support.
     *
     * @param eventType The event type to filter by
     * @param limit The maximum number of events to retrieve
     * @param offset The number of events to skip
     * @return A list of stored events
     */
    suspend fun findByEventType(eventType: String, limit: Int, offset: Int): List<PersistedEventRecord>

    /**
     * Finds events within a time range with pagination support.
     *
     * @param from The start timestamp (inclusive, based on occurredAt)
     * @param to The end timestamp (exclusive, based on occurredAt)
     * @param limit The maximum number of events to retrieve
     * @param offset The number of events to skip
     * @return A list of stored events
     */
    suspend fun findByTimeRange(from: Instant, to: Instant, limit: Int, offset: Int): List<PersistedEventRecord>

    // ===== OPTIMIZATION METHODS FOR LONG-LIVED AGGREGATES =====

    /**
     * Gets the latest version number for an aggregate.
     * Optimized for long-lived aggregates to avoid loading all events.
     *
     * @param aggregateId The aggregate ID to check
     * @return Either an error or the latest version number (null if no events exist)
     */
    suspend fun getLatestAggregateVersion(aggregateId: AggregateId): Either<EventStoreError, Long?>

    /**
     * Gets events from a specific version onwards.
     * Useful for incremental loading of long-lived aggregates.
     *
     * @param aggregateId The aggregate ID to filter by
     * @param fromVersion The minimum version to retrieve (inclusive)
     * @param limit Optional limit on number of events to retrieve
     * @return Either an error or a list of stored events
     */
    suspend fun getEventsByAggregateFromVersion(
        aggregateId: AggregateId, 
        fromVersion: Long, 
        limit: Int? = null
    ): Either<EventStoreError, List<PersistedEventRecord>>

    /**
     * Gets events within a specific version range.
     * Useful for partial replay of long-lived aggregates.
     *
     * @param aggregateId The aggregate ID to filter by
     * @param fromVersion The minimum version to retrieve (inclusive)
     * @param toVersion The maximum version to retrieve (inclusive)
     * @param limit Optional limit on number of events to retrieve
     * @return Either an error or a list of stored events
     */
    suspend fun getEventsByAggregateVersionRange(
        aggregateId: AggregateId,
        fromVersion: Long,
        toVersion: Long,
        limit: Int? = null
    ): Either<EventStoreError, List<PersistedEventRecord>>

    /**
     * Gets the latest N events for an aggregate.
     * Useful for recent activity on long-lived aggregates.
     *
     * @param aggregateId The aggregate ID to filter by
     * @param limit The maximum number of recent events to retrieve
     * @return Either an error or a list of stored events (newest first)
     */
    suspend fun getLatestEventsByAggregate(
        aggregateId: AggregateId, 
        limit: Int
    ): Either<EventStoreError, List<PersistedEventRecord>>

    /**
     * Counts total events for an aggregate.
     * Useful for performance monitoring and snapshot decision making.
     *
     * @param aggregateId The aggregate ID to count events for
     * @return Either an error or the total event count
     */
    suspend fun countEventsByAggregate(aggregateId: AggregateId): Either<EventStoreError, Long>

    /**
     * Gets statistical information about an aggregate's events.
     * Useful for snapshot decision making and performance monitoring.
     *
     * @param aggregateId The aggregate ID to analyze
     * @return Either an error or statistics about the aggregate's events
     */
    suspend fun getAggregateEventStats(aggregateId: AggregateId): Either<EventStoreError, AggregateEventStats>
}

/**
 * Statistical information about an aggregate's events.
 * Used for performance monitoring and snapshot optimization decisions.
 */
data class AggregateEventStats(
    val totalEvents: Long,
    val minVersion: Long?,
    val maxVersion: Long?,
    val firstEventTime: Instant?,
    val lastEventTime: Instant?
)
