package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.eventstore.domain.repository.EventRepository
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.EventSourcingRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.EventStoreErrorMapper
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours

/**
 * Implementation of EventSourcingRepository using the EventStore bounded context.
 *
 * This adapter bridges between the scope-management domain's event sourcing needs
 * and the dedicated event-store infrastructure. It handles:
 * - Storing domain events with optimistic concurrency control
 * - Retrieving events for aggregate reconstruction
 * - Mapping errors between contexts
 *
 * Thread-safety: This implementation is thread-safe as it delegates to EventRepository
 * which handles concurrency at the database level.
 */
internal class EventStoreScopeEventSourcingRepository(private val eventRepository: EventRepository, logger: Logger) : EventSourcingRepository<DomainEvent> {

    private val eventStoreErrorMapper = EventStoreErrorMapper(logger)

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
            eventRepository.store(event)
                .mapLeft { eventStoreErrorMapper.mapCrossContext(it) }
                .bind()
        }
    }

    override suspend fun getEvents(aggregateId: AggregateId): Either<ScopesError, List<DomainEvent>> = eventRepository.getEventsByAggregate(aggregateId)
        .mapLeft { eventStoreErrorMapper.mapCrossContext(it) }
        .map { persistedEvents ->
            persistedEvents.map { it.event }
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

    override suspend fun getEventsByType(eventType: String, limit: Int, offset: Int): Either<ScopesError, List<DomainEvent>> =
        // Get all events and filter by type
        // This is inefficient but EventRepository doesn't support type filtering yet
        eventRepository.getEventsSince(Instant.DISTANT_PAST, limit = limit + offset)
            .mapLeft { eventStoreErrorMapper.mapCrossContext(it) }
            .map { persistedEvents ->
                persistedEvents
                    .filter { it.event::class.simpleName == eventType || it.event::class.qualifiedName == eventType }
                    .drop(offset)
                    .take(limit)
                    .map { it.event }
            }

    override suspend fun getEventsByTimeRange(from: Instant, to: Instant, limit: Int, offset: Int): Either<ScopesError, List<DomainEvent>> =
        // Since EventRepository.getEventsSince filters by stored_at, we need to fetch a larger window
        // to ensure we don't miss events with occurredAt in our range but stored later.
        // We'll fetch all events since 'from' minus 1 hour buffer and filter by occurredAt in memory.
        eventRepository.getEventsSince(from - 1.hours, limit = null)
            .mapLeft { eventStoreErrorMapper.mapCrossContext(it) }
            .map { persistedEvents ->
                persistedEvents
                    .filter { it.metadata.occurredAt >= from && it.metadata.occurredAt < to }
                    .drop(offset)
                    .take(limit)
                    .map { it.event }
            }

    override suspend fun saveSnapshot(aggregateId: AggregateId, snapshot: ByteArray, version: Int): Either<ScopesError, Unit> =
        // Snapshots not yet implemented in EventRepository
        Either.Right(Unit)

    override suspend fun getLatestSnapshot(aggregateId: AggregateId): Either<ScopesError, Pair<ByteArray, Int>?> =
        // Snapshots not yet implemented in EventRepository
        Either.Right(null)
}
