package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreCommandPort
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreQueryPort
import io.github.kamiazya.scopes.contracts.eventstore.commands.StoreEventCommand
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsByAggregateQuery
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsByTimeRangeQuery
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsByTypeQuery
import io.github.kamiazya.scopes.contracts.eventstore.results.EventResult
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.EventSourcingRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.EventStoreContractErrorMapper
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementation of EventSourcingRepository using EventStore contracts.
 *
 * This implementation decouples the scope-management context from the event-store
 * domain by using only the public contract interfaces. This ensures:
 * - Clean bounded context separation
 * - Stable API boundaries
 * - Easy testing with contract mocks
 *
 * The implementation handles:
 * - Event serialization/deserialization
 * - Version checking for optimistic concurrency
 * - Error mapping between contexts
 */
internal class ContractBasedScopeEventSourcingRepository(
    private val eventStoreCommandPort: EventStoreCommandPort,
    private val eventStoreQueryPort: EventStoreQueryPort,
    private val eventStoreContractErrorMapper: EventStoreContractErrorMapper,
    private val json: Json,
) : EventSourcingRepository<DomainEvent> {

    override suspend fun saveEvents(aggregateId: AggregateId, events: List<DomainEvent>, expectedVersion: Int): Either<ScopesError, Unit> = either {
        // Validate expected version matches current version
        val currentVersion = getCurrentVersion(aggregateId).bind()
        if (currentVersion != expectedVersion) {
            raise(
                ScopesError.ConcurrencyError(
                    aggregateId = aggregateId.value,
                    aggregateType = "Scope",
                    expectedVersion = expectedVersion,
                    actualVersion = currentVersion,
                    operation = "saveEvents",
                ),
            )
        }

        // Store events sequentially to maintain order
        events.forEach { event ->
            val command = StoreEventCommand(
                aggregateId = event.aggregateId.value,
                aggregateVersion = event.aggregateVersion.value,
                eventType = event::class.simpleName ?: error("Event class must have a simple name"),
                eventData = json.encodeToString(event),
                occurredAt = event.occurredAt,
                metadata = mapOf(
                    "eventId" to event.eventId.value,
                ),
            )

            eventStoreCommandPort.createEvent(command)
                .mapLeft { eventStoreContractErrorMapper.mapCrossContext(it) }
                .bind()
        }
    }

    override suspend fun getEvents(aggregateId: AggregateId): Either<ScopesError, List<DomainEvent>> {
        val query = GetEventsByAggregateQuery(
            aggregateId = aggregateId.value,
            since = null,
            limit = null,
        )

        return eventStoreQueryPort.getEventsByAggregate(query)
            .mapLeft { eventStoreContractErrorMapper.mapCrossContext(it) }
            .map { results -> results.mapNotNull { deserializeEvent(it) } }
    }

    override suspend fun getEventsFromVersion(aggregateId: AggregateId, fromVersion: Int): Either<ScopesError, List<DomainEvent>> =
        getEvents(aggregateId).map { events ->
            events.filter { event ->
                event.aggregateVersion.value >= fromVersion
            }
        }

    override suspend fun getEventsBetweenVersions(aggregateId: AggregateId, fromVersion: Int, toVersion: Int): Either<ScopesError, List<DomainEvent>> =
        getEvents(aggregateId).map { events ->
            events.filter { event ->
                event.aggregateVersion.value in fromVersion..toVersion
            }
        }

    override suspend fun getCurrentVersion(aggregateId: AggregateId): Either<ScopesError, Int> = getEvents(aggregateId).map { events ->
        events.maxOfOrNull { it.aggregateVersion.value.toInt() } ?: 0
    }

    override suspend fun exists(aggregateId: AggregateId): Either<ScopesError, Boolean> = getEvents(aggregateId).map { events ->
        events.isNotEmpty()
    }

    override suspend fun getEventsByType(eventType: String, limit: Int, offset: Int): Either<ScopesError, List<DomainEvent>> {
        val query = GetEventsByTypeQuery(
            eventType = eventType,
            limit = limit,
            offset = offset,
        )

        return eventStoreQueryPort.getEventsByType(query)
            .mapLeft { eventStoreContractErrorMapper.mapCrossContext(it) }
            .map { results -> results.mapNotNull { deserializeEvent(it) } }
    }

    override suspend fun getEventsByTimeRange(from: Instant, to: Instant, limit: Int, offset: Int): Either<ScopesError, List<DomainEvent>> {
        val query = GetEventsByTimeRangeQuery(
            from = from,
            to = to,
            limit = limit,
            offset = offset,
        )

        return eventStoreQueryPort.getEventsByTimeRange(query)
            .mapLeft { eventStoreContractErrorMapper.mapCrossContext(it) }
            .map { results -> results.mapNotNull { deserializeEvent(it) } }
    }

    override suspend fun saveSnapshot(aggregateId: AggregateId, snapshot: ByteArray, version: Int): Either<ScopesError, Unit> =
        // Snapshots not yet implemented in EventStore contracts
        Either.Right(Unit)

    override suspend fun getLatestSnapshot(aggregateId: AggregateId): Either<ScopesError, Pair<ByteArray, Int>?> =
        // Snapshots not yet implemented in EventStore contracts
        Either.Right(null)

    /**
     * Deserializes an EventResult back to a DomainEvent.
     * Returns null if deserialization fails (event type unknown or data corrupted).
     */
    private fun deserializeEvent(result: EventResult): DomainEvent? = try {
        // In a real implementation, you would use a registry or reflection
        // to map event types to their classes. For now, we'll deserialize
        // and return the event as-is since it already contains all the fields
        json.decodeFromString<DomainEvent>(result.eventData)
    } catch (e: Exception) {
        // Log the error and skip this event
        // In production, this should be handled more gracefully
        null
    }
}
