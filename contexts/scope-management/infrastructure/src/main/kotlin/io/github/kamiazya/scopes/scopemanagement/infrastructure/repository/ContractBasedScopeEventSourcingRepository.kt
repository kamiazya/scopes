package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreCommandPort
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreQueryPort
import io.github.kamiazya.scopes.contracts.eventstore.commands.StoreEventCommand
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsByAggregateFromVersionQuery
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsByAggregateQuery
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsByAggregateVersionRangeQuery
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsByTimeRangeQuery
import io.github.kamiazya.scopes.contracts.eventstore.queries.GetEventsByTypeQuery
import io.github.kamiazya.scopes.contracts.eventstore.results.EventResult
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.event.EventEnvelope
import io.github.kamiazya.scopes.platform.domain.event.VersionSupport
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
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
                eventType = eventTypeId(event),
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

    override suspend fun saveEventsWithVersioning(
        aggregateId: AggregateId,
        events: List<EventEnvelope.Pending<DomainEvent>>,
        expectedVersion: Int,
    ): Either<ScopesError, List<EventEnvelope.Persisted<DomainEvent>>> = either {
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

        // Store events sequentially and assign versions
        val persistedEvents = events.mapIndexed { index, pendingEnvelope ->
            val newVersion = AggregateVersion.fromUnsafe((expectedVersion + index + 1).toLong())
            val event = pendingEnvelope.event

            // Create a new event with the correct version using type-safe VersionSupport
            val eventWithVersion = (event as? VersionSupport<DomainEvent>)?.withVersion(newVersion)
                ?: raise(
                    ScopesError.SystemError(
                        errorType = ScopesError.SystemError.SystemErrorType.SERIALIZATION_FAILED,
                        service = "EventSourcingRepository",
                        context = mapOf(
                            "aggregateId" to aggregateId.value,
                            "eventType" to (event::class.simpleName ?: event::class.java.name),
                            "eventId" to event.eventId.value,
                        ),
                    ),
                )

            val command = StoreEventCommand(
                aggregateId = eventWithVersion.aggregateId.value,
                aggregateVersion = eventWithVersion.aggregateVersion.value,
                eventType = eventTypeId(eventWithVersion),
                eventData = json.encodeToString(eventWithVersion),
                occurredAt = eventWithVersion.occurredAt,
                metadata = mapOf(
                    "eventId" to eventWithVersion.eventId.value,
                ),
            )

            eventStoreCommandPort.createEvent(command)
                .mapLeft { eventStoreContractErrorMapper.mapCrossContext(it) }
                .bind()

            EventEnvelope.Persisted(eventWithVersion, newVersion)
        }

        persistedEvents
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

    override suspend fun getEventsFromVersion(aggregateId: AggregateId, fromVersion: Int): Either<ScopesError, List<DomainEvent>> {
        val query = GetEventsByAggregateFromVersionQuery(
            aggregateId = aggregateId.value,
            fromVersion = fromVersion,
            limit = null,
        )

        return eventStoreQueryPort.getEventsByAggregateFromVersion(query)
            .mapLeft { eventStoreContractErrorMapper.mapCrossContext(it) }
            .map { results -> results.mapNotNull { deserializeEvent(it) } }
    }

    override suspend fun getEventsBetweenVersions(aggregateId: AggregateId, fromVersion: Int, toVersion: Int): Either<ScopesError, List<DomainEvent>> {
        val query = GetEventsByAggregateVersionRangeQuery(
            aggregateId = aggregateId.value,
            fromVersion = fromVersion,
            toVersion = toVersion,
            limit = null,
        )

        return eventStoreQueryPort.getEventsByAggregateVersionRange(query)
            .mapLeft { eventStoreContractErrorMapper.mapCrossContext(it) }
            .map { results -> results.mapNotNull { deserializeEvent(it) } }
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

    private fun eventTypeId(event: DomainEvent): String {
        // Prefer platform-level @EventTypeId; fallback to class name
        val ann = event::class.annotations.firstOrNull { it is io.github.kamiazya.scopes.platform.domain.event.EventTypeId }
        if (ann is io.github.kamiazya.scopes.platform.domain.event.EventTypeId) {
            return ann.value
        }
        return event::class.qualifiedName
            ?: (event::class.simpleName ?: error("Event class must have a name"))
    }
}
