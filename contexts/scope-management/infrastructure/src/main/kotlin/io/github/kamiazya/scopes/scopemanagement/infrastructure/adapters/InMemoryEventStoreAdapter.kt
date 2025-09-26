package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import arrow.core.Either
import arrow.core.right
import arrow.core.left
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreCommandPort
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreQueryPort
import io.github.kamiazya.scopes.contracts.eventstore.commands.StoreEventCommand
import io.github.kamiazya.scopes.contracts.eventstore.errors.EventStoreContractError
import io.github.kamiazya.scopes.contracts.eventstore.queries.*
import io.github.kamiazya.scopes.contracts.eventstore.results.EventResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory implementation of EventStore contracts for testing.
 * 
 * This adapter provides a thread-safe, in-memory event store that
 * implements both command and query ports. It's designed for:
 * - Unit and integration testing
 * - Development environments
 * - Demonstration purposes
 * 
 * Features:
 * - Thread-safe operations using concurrent data structures
 * - Optimistic concurrency control with version checking
 * - Query support by aggregate, version, type, and time range
 * - Proper error handling for concurrency conflicts
 */
class InMemoryEventStoreAdapter(
    private val json: Json
) : EventStoreCommandPort, EventStoreQueryPort {
    
    // Storage: aggregateId -> list of events
    private val eventsByAggregate = ConcurrentHashMap<String, CopyOnWriteArrayList<StoredEvent>>()
    
    // Global event list for time-based and type-based queries
    private val allEvents = CopyOnWriteArrayList<StoredEvent>()
    
    // Mutex for write operations to ensure version consistency
    private val writeMutex = Mutex()
    
    // Counter for generating sequential event IDs
    private var eventIdCounter = 0L
    
    override suspend fun createEvent(command: StoreEventCommand): Either<EventStoreContractError, Unit> = writeMutex.withLock {
        // Get or create event list for this aggregate
        val aggregateEvents = eventsByAggregate.computeIfAbsent(command.aggregateId) { CopyOnWriteArrayList() }
        
        // Check version consistency
        val currentVersion = aggregateEvents.size
        val expectedVersion = command.aggregateVersion.toInt()
        
        if (expectedVersion != currentVersion + 1) {
            return EventStoreContractError.EventStorageError(
                aggregateId = command.aggregateId,
                eventType = command.eventType,
                eventVersion = command.aggregateVersion,
                storageReason = EventStoreContractError.StorageFailureReason.VERSION_CONFLICT,
                conflictingVersion = currentVersion.toLong()
            ).left()
        }
        
        // Create and store the event
        val storedEvent = StoredEvent(
            id = ++eventIdCounter,
            aggregateId = command.aggregateId,
            aggregateVersion = command.aggregateVersion,
            eventType = command.eventType,
            eventData = command.eventData,
            metadata = command.metadata ?: emptyMap(),
            occurredAt = command.occurredAt
        )
        
        aggregateEvents.add(storedEvent)
        allEvents.add(storedEvent)
        
        Unit.right()
    }
    
    override suspend fun getEventsByAggregate(query: GetEventsByAggregateQuery): Either<EventStoreContractError, List<EventResult>> {
        val events = eventsByAggregate[query.aggregateId] ?: return emptyList<EventResult>().right()
        
        var result = events.toList()
        
        // Apply since filter if specified
        query.since?.let { since ->
            result = result.filter { it.occurredAt >= since }
        }
        
        // Apply limit if specified
        query.limit?.let { limit ->
            result = result.take(limit)
        }
        
        return result.map { it.toEventResult() }.right()
    }
    
    override suspend fun getEventsByAggregateFromVersion(query: GetEventsByAggregateFromVersionQuery): Either<EventStoreContractError, List<EventResult>> {
        val events = eventsByAggregate[query.aggregateId] ?: return emptyList<EventResult>().right()
        
        var result = events.filter { it.aggregateVersion.toInt() >= query.fromVersion }
        
        // Apply limit if specified
        query.limit?.let { limit ->
            result = result.take(limit)
        }
        
        return result.map { it.toEventResult() }.right()
    }
    
    override suspend fun getEventsByAggregateVersionRange(query: GetEventsByAggregateVersionRangeQuery): Either<EventStoreContractError, List<EventResult>> {
        val events = eventsByAggregate[query.aggregateId] ?: return emptyList<EventResult>().right()
        
        var result = events.filter { 
            val version = it.aggregateVersion.toInt()
            version in query.fromVersion..query.toVersion
        }
        
        // Apply limit if specified
        query.limit?.let { limit ->
            result = result.take(limit)
        }
        
        return result.map { it.toEventResult() }.right()
    }
    
    override suspend fun getEventsByType(query: GetEventsByTypeQuery): Either<EventStoreContractError, List<EventResult>> {
        var result = allEvents.filter { it.eventType == query.eventType }
        
        // Apply pagination
        result = result.drop(query.offset).take(query.limit)
        
        return result.map { it.toEventResult() }.right()
    }
    
    override suspend fun getEventsByTimeRange(query: GetEventsByTimeRangeQuery): Either<EventStoreContractError, List<EventResult>> {
        var result = allEvents.filter { it.occurredAt in query.from..query.to }
        
        // Sort by time
        result = result.sortedBy { it.occurredAt }
        
        // Apply pagination
        result = result.drop(query.offset).take(query.limit)
        
        return result.map { it.toEventResult() }.right()
    }
    
    override suspend fun getEventsSince(query: GetEventsSinceQuery): Either<EventStoreContractError, List<EventResult>> {
        var result = allEvents.filter { it.occurredAt >= query.since }
        
        // Sort by time
        result = result.sortedBy { it.occurredAt }
        
        // Apply limit if specified
        query.limit?.let { limit ->
            result = result.take(limit)
        }
        
        return result.map { it.toEventResult() }.right()
    }
    
    /**
     * Internal storage class for events
     */
    private data class StoredEvent(
        val id: Long,
        val aggregateId: String,
        val aggregateVersion: Long,
        val eventType: String,
        val eventData: String,
        val metadata: Map<String, String>,
        val occurredAt: Instant,
        val storedAt: Instant = Clock.System.now()
    ) {
        fun toEventResult() = EventResult(
            eventId = id.toString(),
            aggregateId = aggregateId,
            aggregateVersion = aggregateVersion,
            eventType = eventType,
            eventData = eventData,
            metadata = metadata,
            occurredAt = occurredAt,
            storedAt = storedAt,
            sequenceNumber = id
        )
    }
    
    /**
     * Test helper methods
     */
    fun clear() {
        eventsByAggregate.clear()
        allEvents.clear()
        eventIdCounter = 0L
    }
    
    fun getEventCount(): Int = allEvents.size
    
    fun getAggregateCount(): Int = eventsByAggregate.size
}