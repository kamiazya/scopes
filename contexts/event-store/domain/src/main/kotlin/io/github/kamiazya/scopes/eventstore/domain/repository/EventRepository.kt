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
}
