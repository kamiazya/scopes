package io.github.kamiazya.scopes.eventstore.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.eventstore.domain.entity.PersistedEventRecord
import io.github.kamiazya.scopes.eventstore.domain.error.EventStoreDomainError
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId

/**
 * Domain service for validating event store operations and enforcing business rules.
 *
 * This service ensures event ordering, version consistency, and prevents duplicate events.
 * It is a pure domain service that contains no I/O operations and can be easily tested.
 *
 * @since 1.0.0
 */
interface EventValidationService {
    /**
     * Validates that a new event can be appended to the event stream.
     *
     * This method ensures:
     * - For new aggregates: expectedVersion must be 1
     * - For existing aggregates: expectedVersion must be currentVersion + 1
     *
     * @param eventId The unique identifier of the event to append
     * @param aggregateId The aggregate this event belongs to
     * @param expectedVersion The version number this event expects to have
     * @param currentVersion The current version of the aggregate (null for new aggregates)
     * @return Either an [EventStoreDomainError.InvalidAggregateVersion] or Unit on success
     */
    fun validateEventAppend(
        eventId: EventId,
        aggregateId: AggregateId,
        expectedVersion: AggregateVersion,
        currentVersion: AggregateVersion?,
    ): Either<EventStoreDomainError, Unit>

    /**
     * Validates event ordering within an aggregate's event stream.
     *
     * Checks for:
     * - Version gaps (missing events)
     * - Version duplicates (same version appears twice)
     * - Chronological consistency (later versions have later timestamps)
     *
     * @param events List of persisted events to validate (can be from multiple aggregates)
     * @return Either an [EventStoreDomainError.EventOrderingViolation] or Unit on success
     */
    fun validateEventOrdering(events: List<PersistedEventRecord>): Either<EventStoreDomainError, Unit>

    /**
     * Validates if an event ID would be a duplicate.
     *
     * Note: This is pure validation logic. The actual duplicate check against
     * the database should be performed by the application/infrastructure layer.
     *
     * @param eventId The event ID to validate
     * @param existingEventIds Set of existing event IDs (provided by app layer)
     * @return Either an [EventStoreDomainError.DuplicateEvent] or Unit if valid
     */
    fun validateEventNotDuplicate(eventId: EventId, existingEventIds: Set<EventId>): Either<EventStoreDomainError, Unit>
}

/**
 * Default implementation of EventValidationService with strict validation rules.
 *
 * This implementation enforces:
 * - Strict version sequencing (no gaps, no duplicates)
 * - Chronological consistency within aggregates
 * - Event uniqueness by ID
 *
 * @since 1.0.0
 */
class DefaultEventValidationService : EventValidationService {

    override fun validateEventAppend(
        eventId: EventId,
        aggregateId: AggregateId,
        expectedVersion: AggregateVersion,
        currentVersion: AggregateVersion?,
    ): Either<EventStoreDomainError, Unit> {
        // For new aggregates, expected version should be 1
        if (currentVersion == null) {
            return if (expectedVersion.value == 1L) {
                Unit.right()
            } else {
                EventStoreDomainError.InvalidAggregateVersion(
                    aggregateId = aggregateId,
                    expectedVersion = 1L,
                    actualVersion = expectedVersion.value,
                ).left()
            }
        }

        // For existing aggregates, expected version should be current + 1
        val nextVersion = currentVersion.value + 1
        return if (expectedVersion.value == nextVersion) {
            Unit.right()
        } else {
            EventStoreDomainError.InvalidAggregateVersion(
                aggregateId = aggregateId,
                expectedVersion = nextVersion,
                actualVersion = expectedVersion.value,
            ).left()
        }
    }

    override fun validateEventOrdering(events: List<PersistedEventRecord>): Either<EventStoreDomainError, Unit> {
        if (events.isEmpty()) return Unit.right()

        // Group events by aggregate
        val eventsByAggregate = events.groupBy { it.metadata.aggregateId }

        // Validate each aggregate's event stream
        for ((aggregateId, aggregateEvents) in eventsByAggregate) {
            val sortedEvents = aggregateEvents.sortedBy { it.metadata.aggregateVersion.value }

            // Check for version gaps or duplicates
            var expectedVersion = sortedEvents.first().metadata.aggregateVersion.value
            for (event in sortedEvents) {
                val actualVersion = event.metadata.aggregateVersion.value
                if (actualVersion != expectedVersion) {
                    return EventStoreDomainError.EventOrderingViolation(
                        aggregateId = aggregateId,
                        message = "Expected version $expectedVersion but got $actualVersion",
                    ).left()
                }
                expectedVersion++
            }

            // Check chronological ordering
            for (i in 1 until sortedEvents.size) {
                val prev = sortedEvents[i - 1]
                val curr = sortedEvents[i]
                if (curr.metadata.occurredAt < prev.metadata.occurredAt) {
                    return EventStoreDomainError.EventOrderingViolation(
                        aggregateId = aggregateId,
                        message = "Event ${curr.metadata.eventId.value} occurred before ${prev.metadata.eventId.value} but has higher version",
                    ).left()
                }
            }
        }

        return Unit.right()
    }

    override fun validateEventNotDuplicate(eventId: EventId, existingEventIds: Set<EventId>): Either<EventStoreDomainError, Unit> =
        if (eventId in existingEventIds) {
            EventStoreDomainError.DuplicateEvent(
                eventId = eventId,
            ).left()
        } else {
            Unit.right()
        }
}

/**
 * Domain service for enforcing event type compatibility and schema evolution rules.
 *
 * This service ensures that only known event types are stored and that events
 * are compatible with the aggregates they're being applied to.
 *
 * @since 1.0.0
 */
interface EventTypeValidationService {
    /**
     * Validates that an event type is registered and can be stored.
     *
     * This prevents unknown event types from being persisted, which could
     * cause deserialization issues when replaying events.
     *
     * @param eventType The fully qualified class name of the event
     * @param registeredTypes Set of registered event types (provided by app layer)
     * @return Either an [EventStoreDomainError.InvalidEventType] or Unit on success
     */
    fun validateEventType(eventType: String, registeredTypes: Set<String>): Either<EventStoreDomainError, Unit>

    /**
     * Validates that an event can be applied to an aggregate of a specific type.
     *
     * Ensures type safety by preventing incompatible events from being applied
     * to aggregates (e.g., preventing a UserCreated event from being applied to
     * an Order aggregate).
     *
     * @param eventType The fully qualified class name of the event
     * @param aggregateType The type of aggregate this event will be applied to
     * @param compatibilityRules Map of aggregate types to their compatible event types
     * @return Either an [EventStoreDomainError.IncompatibleEventType] or Unit on success
     */
    fun validateEventForAggregateType(
        eventType: String,
        aggregateType: String,
        compatibilityRules: Map<String, Set<String>>,
    ): Either<EventStoreDomainError, Unit>
}
