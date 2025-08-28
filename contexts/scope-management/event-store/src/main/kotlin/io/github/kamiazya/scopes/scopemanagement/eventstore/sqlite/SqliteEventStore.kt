@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package io.github.kamiazya.scopes.scopemanagement.eventstore.sqlite

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import co.touchlab.kermit.Logger
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.scopemanagement.eventstore.EventStore
import io.github.kamiazya.scopes.scopemanagement.eventstore.EventStoreError
import io.github.kamiazya.scopes.scopemanagement.eventstore.model.EventMetadata
import io.github.kamiazya.scopes.scopemanagement.eventstore.model.StoredEvent
import io.github.kamiazya.scopes.scopemanagement.eventstore.model.VectorClock
import io.github.kamiazya.scopes.scopemanagement.eventstore.sqlite.Events
import io.github.kamiazya.scopes.scopemanagement.eventstore.sqlite.VectorClocks
import io.github.kamiazya.scopes.scopemanagement.eventstore.valueobject.DeviceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.uuid.Uuid

/**
 * SQLite-based implementation of EventStore for local event persistence.
 */
class SqliteEventStore(private val database: Database, private val json: Json, private val logger: Logger) : EventStore {

    private val eventFlow = MutableSharedFlow<StoredEvent>()

    init {
        transaction(database) {
            SchemaUtils.create(Events, VectorClocks)
        }
    }

    override suspend fun store(event: DomainEvent, deviceId: DeviceId, vectorClock: VectorClock): Either<EventStoreError, StoredEvent> =
        withContext(Dispatchers.IO) {
            try {
                newSuspendedTransaction(Dispatchers.IO, database) {
                    // Generate event ID and get next sequence number
                    val eventId = Uuid.random().toString()
                    val sequenceNumber = Events
                        .select(Events.sequenceNumber)
                        .where { Events.deviceId eq deviceId.value }
                        .orderBy(Events.sequenceNumber to SortOrder.DESC)
                        .limit(1)
                        .singleOrNull()
                        ?.get(Events.sequenceNumber)
                        ?.plus(1) ?: 1L

                    // Serialize event data
                    val eventData = json.encodeToString(event)
                    val eventType = event::class.simpleName ?: "Unknown"
                    val aggregateId = extractAggregateId(event)

                    // Insert event
                    Events.insert {
                        it[this.eventId] = eventId
                        it[this.aggregateId] = aggregateId
                        it[this.eventType] = eventType
                        it[this.deviceId] = deviceId.value
                        it[this.sequenceNumber] = sequenceNumber
                        it[this.timestamp] = event.occurredAt
                        it[this.eventData] = eventData
                        it[this.vectorClock] = json.encodeToString(vectorClock)
                    }

                    // Update vector clocks table
                    VectorClocks.deleteWhere { VectorClocks.deviceId eq deviceId.value }
                    VectorClocks.insert {
                        it[this.deviceId] = deviceId.value
                        it[this.vectorClock] = json.encodeToString(vectorClock)
                        it[this.lastUpdated] = Clock.System.now()
                    }

                    val metadata = EventMetadata(
                        eventId = eventId,
                        aggregateId = aggregateId,
                        eventType = eventType,
                        deviceId = deviceId.value,
                        vectorClock = vectorClock,
                        timestamp = event.occurredAt,
                        sequenceNumber = sequenceNumber,
                    )

                    val storedEvent = StoredEvent(metadata, event)

                    // Emit to flow
                    eventFlow.emit(storedEvent)

                    storedEvent.right()
                }
            } catch (e: Exception) {
                logger.e("Failed to store event", e)
                EventStoreError.DatabaseError(
                    occurredAt = Clock.System.now(),
                    operation = "store",
                    cause = e,
                ).left()
            }
        }

    override suspend fun getEventsSince(since: Instant, deviceId: DeviceId?): Either<EventStoreError, List<StoredEvent>> = withContext(Dispatchers.IO) {
        try {
            newSuspendedTransaction(Dispatchers.IO, database) {
                val query = Events.selectAll().where { Events.timestamp greater since }

                if (deviceId != null) {
                    query.andWhere { Events.deviceId eq deviceId.value }
                }

                val events = query.orderBy(Events.timestamp to SortOrder.ASC)
                    .map { row -> deserializeStoredEvent(row) }
                    .filterNotNull()
                events.right()
            }
        } catch (e: Exception) {
            logger.e("Failed to get events since $since", e)
            EventStoreError.DatabaseError(
                occurredAt = Clock.System.now(),
                operation = "getEventsSince",
                cause = e,
            ).left()
        }
    }

    override suspend fun getEventsByAggregate(aggregateId: String, since: Instant?): Either<EventStoreError, List<StoredEvent>> = withContext(Dispatchers.IO) {
        try {
            newSuspendedTransaction(Dispatchers.IO, database) {
                val query = Events.selectAll().where { Events.aggregateId eq aggregateId }

                if (since != null) {
                    query.andWhere { Events.timestamp greater since }
                }

                val events = query.orderBy(Events.timestamp to SortOrder.ASC)
                    .map { row -> deserializeStoredEvent(row) }
                    .filterNotNull()
                events.right()
            }
        } catch (e: Exception) {
            logger.e("Failed to get events for aggregate $aggregateId", e)
            EventStoreError.DatabaseError(
                occurredAt = Clock.System.now(),
                operation = "getEventsByAggregate",
                cause = e,
            ).left()
        }
    }

    override suspend fun getCurrentVectorClock(deviceId: DeviceId): Either<EventStoreError, VectorClock> = withContext(Dispatchers.IO) {
        try {
            newSuspendedTransaction(Dispatchers.IO, database) {
                val row = VectorClocks.selectAll()
                    .where { VectorClocks.deviceId eq deviceId.value }
                    .singleOrNull()

                if (row != null) {
                    json.decodeFromString<VectorClock>(row[VectorClocks.vectorClock]).right()
                } else {
                    // Return empty vector clock if none exists
                    VectorClock.empty().right()
                }
            }
        } catch (e: Exception) {
            logger.e("Failed to get vector clock for device $deviceId", e)
            EventStoreError.DatabaseError(
                occurredAt = Clock.System.now(),
                operation = "getCurrentVectorClock",
                cause = e,
            ).left()
        }
    }

    override suspend fun updateVectorClock(deviceId: DeviceId, remoteVectorClock: VectorClock): Either<EventStoreError, VectorClock> =
        withContext(Dispatchers.IO) {
            try {
                newSuspendedTransaction(Dispatchers.IO, database) {
                    val currentClock = getCurrentVectorClock(deviceId).fold(
                        { VectorClock.empty() },
                        { it },
                    )

                    val mergedClock = currentClock.merge(remoteVectorClock)

                    VectorClocks.deleteWhere { VectorClocks.deviceId eq deviceId.value }
                    VectorClocks.insert {
                        it[this.deviceId] = deviceId.value
                        it[this.vectorClock] = json.encodeToString(mergedClock)
                        it[this.lastUpdated] = Clock.System.now()
                    }

                    mergedClock.right()
                }
            } catch (e: Exception) {
                logger.e("Failed to update vector clock for device $deviceId", e)
                EventStoreError.DatabaseError(
                    occurredAt = Clock.System.now(),
                    operation = "updateVectorClock",
                    cause = e,
                ).left()
            }
        }

    override fun streamEvents(): Flow<StoredEvent> = eventFlow

    override fun hasConflicts(localClock: VectorClock, remoteClock: VectorClock): Boolean = localClock.isConcurrentWith(remoteClock)

    override suspend fun findConflictingEvents(
        localDeviceId: DeviceId,
        remoteEvents: List<StoredEvent>,
    ): Either<EventStoreError, List<Pair<StoredEvent, StoredEvent>>> = withContext(Dispatchers.IO) {
        try {
            val conflicts = mutableListOf<Pair<StoredEvent, StoredEvent>>()

            newSuspendedTransaction(Dispatchers.IO, database) {
                for (remoteEvent in remoteEvents) {
                    // Find local events for the same aggregate
                    val localEvents = Events.selectAll()
                        .where {
                            (Events.aggregateId eq remoteEvent.metadata.aggregateId) and
                                (Events.deviceId eq localDeviceId.value)
                        }
                        .map { row -> deserializeStoredEvent(row) }
                        .filterNotNull()

                    // Check for concurrent events
                    for (localEvent in localEvents) {
                        if (localEvent.isConcurrentWith(remoteEvent)) {
                            conflicts.add(localEvent to remoteEvent)
                        }
                    }
                }
            }

            conflicts.right()
        } catch (e: Exception) {
            logger.e("Failed to find conflicting events", e)
            EventStoreError.DatabaseError(
                occurredAt = Clock.System.now(),
                operation = "findConflictingEvents",
                cause = e,
            ).left()
        }
    }

    private fun deserializeStoredEvent(row: ResultRow): StoredEvent? = try {
        val metadata = EventMetadata(
            eventId = row[Events.eventId],
            aggregateId = row[Events.aggregateId],
            eventType = row[Events.eventType],
            deviceId = row[Events.deviceId],
            vectorClock = json.decodeFromString(row[Events.vectorClock]),
            timestamp = row[Events.timestamp],
            sequenceNumber = row[Events.sequenceNumber],
        )

        // Deserialize the domain event based on type
        val event = deserializeDomainEvent(
            row[Events.eventType],
            row[Events.eventData],
        )

        event?.let { StoredEvent(metadata, it) }
    } catch (e: Exception) {
        logger.e("Failed to deserialize event", e)
        null
    }

    private fun deserializeDomainEvent(eventType: String, eventData: String): DomainEvent? {
        // This is a simplified version - in a real implementation, you'd have a registry
        // of event types and their deserializers
        return try {
            json.decodeFromString<DomainEvent>(eventData)
        } catch (e: Exception) {
            logger.e("Failed to deserialize domain event of type $eventType", e)
            null
        }
    }

    private fun extractAggregateId(event: DomainEvent): String {
        // Extract aggregate ID from the domain event
        return event.aggregateId.value
    }
}
