package io.github.kamiazya.scopes.eventstore.infrastructure.repository

import arrow.core.Either
import io.github.kamiazya.scopes.eventstore.application.port.EventSerializer
import io.github.kamiazya.scopes.eventstore.db.EventQueries
import io.github.kamiazya.scopes.eventstore.domain.entity.PersistedEventRecord
import io.github.kamiazya.scopes.eventstore.domain.error.EventStoreError
import io.github.kamiazya.scopes.eventstore.domain.repository.EventRepository
import io.github.kamiazya.scopes.eventstore.domain.valueobject.EventMetadata
import io.github.kamiazya.scopes.eventstore.domain.valueobject.EventType
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi

/**
 * SQLDelight implementation of the EventRepository.
 */
@OptIn(ExperimentalUuidApi::class)
class SqlDelightEventRepository(private val queries: EventQueries, private val eventSerializer: EventSerializer) : EventRepository {

    override suspend fun store(event: DomainEvent): Either<EventStoreError, PersistedEventRecord> = withContext(Dispatchers.IO) {
        // Serialize the event
        when (val serializationResult = eventSerializer.serialize(event)) {
            is Either.Left -> Either.Left(serializationResult.value)
            is Either.Right -> {
                val eventData = serializationResult.value
                val storedAt = Clock.System.now()

                try {
                    // Insert the event (sequence_number is auto-generated)
                    queries.insertEvent(
                        event_id = event.eventId.value,
                        aggregate_id = event.aggregateId.value,
                        aggregate_version = event.aggregateVersion.value,
                        event_type = event::class.qualifiedName ?: event::class.simpleName ?: "UnknownEvent",
                        event_data = eventData,
                        occurred_at = event.occurredAt.toEpochMilliseconds(),
                        stored_at = storedAt.toEpochMilliseconds(),
                    )

                    // Get the inserted event with auto-generated sequence number
                    val insertedEvent = queries.getEventByEventId(event.eventId.value).executeAsOne()
                    val sequenceNumber = insertedEvent.sequence_number

                    // Return the stored event
                    val storedEvent = PersistedEventRecord(
                        metadata = EventMetadata(
                            eventId = event.eventId,
                            aggregateId = event.aggregateId,
                            aggregateVersion = event.aggregateVersion,
                            eventType = EventType(event::class.qualifiedName ?: event::class.simpleName ?: "UnknownEvent"),
                            occurredAt = event.occurredAt,
                            storedAt = storedAt,
                            sequenceNumber = sequenceNumber,
                        ),
                        event = event,
                    )

                    Either.Right(storedEvent)
                } catch (e: Exception) {
                    Either.Left(
                        EventStoreError.StorageError(
                            aggregateId = event.aggregateId.value,
                            eventType = event::class.qualifiedName ?: event::class.simpleName ?: "UnknownEvent",
                            eventVersion = event.aggregateVersion.value,
                            storageFailureType = EventStoreError.StorageFailureType.VALIDATION_FAILED,
                            cause = e,
                        ),
                    )
                }
            }
        }
    }

    override suspend fun getEventsSince(since: Instant, limit: Int?): Either<EventStoreError, List<PersistedEventRecord>> = withContext(Dispatchers.IO) {
        try {
            val events = queries.findEventsSince(since.toEpochMilliseconds())
                .executeAsList()
                .take(limit ?: Int.MAX_VALUE)
                .mapNotNull { row ->
                    when (
                        val result = deserializeEvent(
                            eventId = row.event_id,
                            aggregateId = row.aggregate_id,
                            aggregateVersion = row.aggregate_version,
                            eventType = row.event_type,
                            eventData = row.event_data,
                            occurredAt = row.occurred_at,
                            storedAt = row.stored_at,
                            sequenceNumber = row.sequence_number,
                        )
                    ) {
                        is Either.Right -> result.value
                        is Either.Left -> null // Skip failed deserialization
                    }
                }

            Either.Right(events)
        } catch (e: Exception) {
            Either.Left(
                EventStoreError.PersistenceError(
                    operation = EventStoreError.PersistenceOperation.READ_FROM_DISK,
                    dataType = "Event",
                ),
            )
        }
    }

    override suspend fun getEventsByAggregate(aggregateId: AggregateId, since: Instant?, limit: Int?): Either<EventStoreError, List<PersistedEventRecord>> =
        withContext(Dispatchers.IO) {
            try {
                val events = if (since != null) {
                    queries.findEventsByAggregateIdSince(
                        aggregate_id = aggregateId.value,
                        stored_at = since.toEpochMilliseconds(),
                    ).executeAsList()
                } else {
                    queries.findEventsByAggregateId(aggregateId.value).executeAsList()
                }.take(limit ?: Int.MAX_VALUE)
                    .mapNotNull { row ->
                        when (
                            val result = deserializeEvent(
                                eventId = row.event_id,
                                aggregateId = row.aggregate_id,
                                aggregateVersion = row.aggregate_version,
                                eventType = row.event_type,
                                eventData = row.event_data,
                                occurredAt = row.occurred_at,
                                storedAt = row.stored_at,
                                sequenceNumber = row.sequence_number,
                            )
                        ) {
                            is Either.Right -> result.value
                            is Either.Left -> null // Skip failed deserialization
                        }
                    }

                Either.Right(events)
            } catch (e: Exception) {
                Either.Left(
                    EventStoreError.PersistenceError(
                        operation = EventStoreError.PersistenceOperation.READ_FROM_DISK,
                        dataType = "AggregateEvents",
                    ),
                )
            }
        }

    override fun streamEvents(): Flow<PersistedEventRecord> = flow {
        // This is a simplified implementation
        // In a real system, you might use database triggers or polling
        // For now, we'll just emit all events
        val result = getEventsSince(Instant.DISTANT_PAST)
        when (result) {
            is Either.Right -> {
                result.value.forEach { emit(it) }
            }
            is Either.Left -> {
                throw IllegalStateException("Failed to stream events")
            }
        }
    }

    private fun deserializeEvent(
        eventId: String,
        aggregateId: String,
        aggregateVersion: Long,
        eventType: String,
        eventData: String,
        occurredAt: Long,
        storedAt: Long,
        sequenceNumber: Long,
    ): Either<EventStoreError, PersistedEventRecord> = eventSerializer.deserialize(eventType, eventData)
        .mapLeft { error -> error }
        .map { domainEvent ->
            PersistedEventRecord(
                metadata = EventMetadata(
                    eventId = EventId.fromUnsafe(eventId),
                    aggregateId = AggregateId.from(aggregateId).getOrNull()
                        ?: throw IllegalStateException("Invalid aggregate ID in database"),
                    aggregateVersion = AggregateVersion.fromUnsafe(aggregateVersion),
                    eventType = EventType(eventType),
                    occurredAt = Instant.fromEpochMilliseconds(occurredAt),
                    storedAt = Instant.fromEpochMilliseconds(storedAt),
                    sequenceNumber = sequenceNumber,
                ),
                event = domainEvent,
            )
        }
}
