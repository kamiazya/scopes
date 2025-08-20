package io.github.kamiazya.scopes.domain.aggregate

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.domain.aggregate.AggregateRoot
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.error.*
import io.github.kamiazya.scopes.domain.event.DomainEvent
import io.github.kamiazya.scopes.domain.event.ScopeArchived
import io.github.kamiazya.scopes.domain.event.ScopeAspectAdded
import io.github.kamiazya.scopes.domain.event.ScopeAspectCleared
import io.github.kamiazya.scopes.domain.event.ScopeAspectRemoved
import io.github.kamiazya.scopes.domain.event.ScopeCreated
import io.github.kamiazya.scopes.domain.event.ScopeDeleted
import io.github.kamiazya.scopes.domain.event.ScopeDescriptionUpdated
import io.github.kamiazya.scopes.domain.event.ScopeParentChanged
import io.github.kamiazya.scopes.domain.event.ScopeRestored
import io.github.kamiazya.scopes.domain.event.ScopeTitleUpdated
import io.github.kamiazya.scopes.domain.valueobject.AggregateId
import io.github.kamiazya.scopes.domain.valueobject.AggregateVersion
import io.github.kamiazya.scopes.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.domain.valueobject.Aspects
import io.github.kamiazya.scopes.domain.valueobject.EventId
import io.github.kamiazya.scopes.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.valueobject.ScopeTitle
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Scope aggregate root implementing event sourcing pattern.
 *
 * This aggregate encapsulates all business logic related to Scopes,
 * ensuring that all state changes go through proper domain events.
 * It maintains consistency boundaries and enforces business rules.
 *
 * Design principles:
 * - All state mutations must generate events
 * - Business rules are validated before generating events
 * - The aggregate can be reconstructed from its event history
 * - Commands return new instances (immutability)
 */
data class ScopeAggregate(
    override val id: AggregateId,
    override val version: AggregateVersion,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    val scopeId: ScopeId,
    val title: ScopeTitle,
    val description: ScopeDescription?,
    val parentId: ScopeId?,
    val aspects: Aspects,
    val isArchived: Boolean = false,
    val archivedAt: Instant? = null,
) : AggregateRoot<ScopeAggregate>() {

    companion object {
        /**
         * Creates a new Scope aggregate.
         * This is the command handler for scope creation.
         *
         * @param title The title for the new scope
         * @param description Optional description
         * @param parentId Optional parent scope ID
         * @param aspectsData Initial aspects to set
         * @param eventId Unique identifier for the creation event
         * @return Either an error or the new aggregate with uncommitted event
         */
        fun create(
            title: String,
            description: String? = null,
            parentId: ScopeId? = null,
            aspectsData: Map<AspectKey, NonEmptyList<AspectValue>> = emptyMap(),
        ): Either<ScopesError, ScopeAggregate> = either {
            // Create the scope entity first to validate all inputs
            val scope = Scope.create(title, description, parentId, aspectsData).bind()

            // Create the aggregate ID
            val aggregateId = scope.id.toAggregateId().bind()

            // Create the event ID
            val eventId = EventId.create(ScopeCreated::class).bind()

            // Create the creation event
            val event = ScopeCreated(
                aggregateId = aggregateId,
                eventId = eventId,
                occurredAt = scope.createdAt,
                version = 1,
                scopeId = scope.id,
                title = scope.title,
                description = scope.description,
                parentId = scope.parentId,
                aspects = scope.getAspects(),
            )

            // Create the initial aggregate state
            val aggregate = ScopeAggregate(
                id = aggregateId,
                version = AggregateVersion.INITIAL, // Will be 1 after event is applied
                createdAt = scope.createdAt,
                updatedAt = scope.createdAt,
                scopeId = scope.id,
                title = scope.title,
                description = scope.description,
                parentId = scope.parentId,
                aspects = scope.aspects,
                isArchived = false,
                archivedAt = null,
            )

            // Apply the event to get the final aggregate with uncommitted event
            aggregate.applyChange(event).bind()
        }

        /**
         * Reconstructs a Scope aggregate from its event history.
         *
         * @param events The chronological list of events for this aggregate
         * @return Either an error or the reconstructed aggregate
         */
        fun fromEvents(events: List<DomainEvent>): Either<ScopesError, ScopeAggregate> = either {
            ensure(events.isNotEmpty()) {
                AggregateIdError.EmptyValue(
                    occurredAt = Clock.System.now(),
                    field = "events",
                )
            }

            val firstEvent = events.first() as? ScopeCreated
            ensureNotNull(firstEvent) {
                AggregateConcurrencyError.InvalidEventSequence(
                    aggregateId = events.first().aggregateId,
                    expectedEventVersion = 1,
                    actualEventVersion = events.first().version,
                )
            }

            // Create initial state from creation event
            val initialAggregate = ScopeAggregate(
                id = firstEvent.aggregateId,
                version = AggregateVersion.INITIAL,
                createdAt = firstEvent.occurredAt,
                updatedAt = firstEvent.occurredAt,
                scopeId = firstEvent.scopeId,
                title = firstEvent.title,
                description = firstEvent.description,
                parentId = firstEvent.parentId,
                aspects = Aspects.from(firstEvent.aspects),
                isArchived = false,
                archivedAt = null,
            )

            // Replay all events
            initialAggregate.replay(events).bind()
        }
    }

    /**
     * Updates the scope title.
     *
     * @param newTitle The new title
     * @param eventId Unique identifier for the event
     * @return Either an error or the updated aggregate with uncommitted event
     */
    fun updateTitle(newTitle: String): Either<ScopesError, ScopeAggregate> = either {
        ensure(!isArchived) {
            ScopeOperationError.OperationOnArchivedScope(
                occurredAt = Clock.System.now(),
                scopeId = scopeId,
                operation = "updateTitle",
            )
        }

        val validatedTitle = ScopeTitle.create(newTitle).bind()

        // Don't generate event if title hasn't changed
        if (title == validatedTitle) {
            return@either this@ScopeAggregate
        }

        val event = newEvent { aggregateId, eventId, occurredAt, version ->
            ScopeTitleUpdated(
                aggregateId = aggregateId,
                eventId = eventId,
                occurredAt = occurredAt,
                version = version,
                scopeId = scopeId,
                oldTitle = title,
                newTitle = validatedTitle,
            )
        }.bind()

        applyChange(event).bind()
    }

    /**
     * Updates the scope description.
     *
     * @param newDescription The new description (null to remove)
     * @param eventId Unique identifier for the event
     * @return Either an error or the updated aggregate with uncommitted event
     */
    fun updateDescription(newDescription: String?): Either<ScopesError, ScopeAggregate> = either {
        ensure(!isArchived) {
            ScopeOperationError.OperationOnArchivedScope(
                occurredAt = Clock.System.now(),
                scopeId = scopeId,
                operation = "updateDescription",
            )
        }

        val validatedDescription = ScopeDescription.create(newDescription).bind()

        // Don't generate event if description hasn't changed
        if (description == validatedDescription) {
            return@either this@ScopeAggregate
        }

        val event = newEvent { aggregateId, eventId, occurredAt, version ->
            ScopeDescriptionUpdated(
                aggregateId = aggregateId,
                eventId = eventId,
                occurredAt = occurredAt,
                version = version,
                scopeId = scopeId,
                oldDescription = description,
                newDescription = validatedDescription,
            )
        }.bind()

        applyChange(event).bind()
    }

    /**
     * Changes the parent of this scope.
     *
     * @param newParentId The new parent ID (null to make it root)
     * @param eventId Unique identifier for the event
     * @return Either an error or the updated aggregate with uncommitted event
     */
    fun changeParent(newParentId: ScopeId?): Either<ScopesError, ScopeAggregate> = either {
        ensure(!isArchived) {
            ScopeOperationError.OperationOnArchivedScope(
                occurredAt = Clock.System.now(),
                scopeId = scopeId,
                operation = "changeParent",
            )
        }

        // Prevent self-parenting
        if (newParentId != null) {
            ensure(newParentId != scopeId) {
                ScopeHierarchyError.SelfParenting(
                    occurredAt = Clock.System.now(),
                    scopeId = scopeId,
                )
            }
        }

        // Don't generate event if parent hasn't changed
        if (parentId == newParentId) {
            return@either this@ScopeAggregate
        }

        val event = newEvent { aggregateId, eventId, occurredAt, version ->
            ScopeParentChanged(
                aggregateId = aggregateId,
                eventId = eventId,
                occurredAt = occurredAt,
                version = version,
                scopeId = scopeId,
                oldParentId = parentId,
                newParentId = newParentId,
            )
        }.bind()

        applyChange(event).bind()
    }

    /**
     * Adds an aspect to the scope.
     *
     * @param key The aspect key
     * @param value The aspect value
     * @param eventId Unique identifier for the event
     * @return Either an error or the updated aggregate with uncommitted event
     */
    fun addAspect(key: AspectKey, value: AspectValue): Either<ScopesError, ScopeAggregate> = either {
        ensure(!isArchived) {
            ScopeOperationError.OperationOnArchivedScope(
                occurredAt = Clock.System.now(),
                scopeId = scopeId,
                operation = "addAspect",
            )
        }

        val event = newEvent { aggregateId, eventId, occurredAt, version ->
            ScopeAspectAdded(
                aggregateId = aggregateId,
                eventId = eventId,
                occurredAt = occurredAt,
                version = version,
                scopeId = scopeId,
                aspectKey = key,
                aspectValue = value,
            )
        }.bind()

        applyChange(event).bind()
    }

    /**
     * Removes a specific aspect value.
     *
     * @param key The aspect key
     * @param value The aspect value to remove
     * @param eventId Unique identifier for the event
     * @return Either an error or the updated aggregate with uncommitted event
     */
    fun removeAspect(key: AspectKey, value: AspectValue): Either<ScopesError, ScopeAggregate> = either {
        ensure(!isArchived) {
            ScopeOperationError.OperationOnArchivedScope(
                occurredAt = Clock.System.now(),
                scopeId = scopeId,
                operation = "removeAspect",
            )
        }

        // Check if the aspect exists
        val currentValues = aspects.get(key)
        ensureNotNull(currentValues) {
            ScopeOperationError.AspectNotFound(
                occurredAt = Clock.System.now(),
                scopeId = scopeId,
                aspectKey = key,
            )
        }

        ensure(currentValues.any { it == value }) {
            ScopeOperationError.AspectValueNotFound(
                occurredAt = Clock.System.now(),
                scopeId = scopeId,
                aspectKey = key,
                aspectValue = value,
            )
        }

        val event = newEvent { aggregateId, eventId, occurredAt, version ->
            ScopeAspectRemoved(
                aggregateId = aggregateId,
                eventId = eventId,
                occurredAt = occurredAt,
                version = version,
                scopeId = scopeId,
                aspectKey = key,
                aspectValue = value,
            )
        }.bind()

        applyChange(event).bind()
    }

    /**
     * Archives the scope (soft delete).
     *
     * @param reason Optional reason for archiving
     * @param eventId Unique identifier for the event
     * @return Either an error or the updated aggregate with uncommitted event
     */
    fun archive(reason: String?): Either<ScopesError, ScopeAggregate> = either {
        ensure(!isArchived) {
            ScopeOperationError.AlreadyArchived(
                occurredAt = Clock.System.now(),
                scopeId = scopeId,
            )
        }

        val event = newEvent { aggregateId, eventId, occurredAt, version ->
            ScopeArchived(
                aggregateId = aggregateId,
                eventId = eventId,
                occurredAt = occurredAt,
                version = version,
                scopeId = scopeId,
                reason = reason,
            )
        }.bind()

        applyChange(event).bind()
    }

    /**
     * Restores an archived scope.
     *
     * @param eventId Unique identifier for the event
     * @return Either an error or the updated aggregate with uncommitted event
     */
    fun restore(): Either<ScopesError, ScopeAggregate> = either {
        ensure(isArchived) {
            ScopeOperationError.NotArchived(
                occurredAt = Clock.System.now(),
                scopeId = scopeId,
            )
        }

        val event = newEvent { aggregateId, eventId, occurredAt, version ->
            ScopeRestored(
                aggregateId = aggregateId,
                eventId = eventId,
                occurredAt = occurredAt,
                version = version,
                scopeId = scopeId,
            )
        }.bind()

        applyChange(event).bind()
    }

    /**
     * Permanently deletes the scope.
     * This generates a final event but the aggregate should not be used after this.
     *
     * @param eventId Unique identifier for the event
     * @return Either an error or the updated aggregate with uncommitted event
     */
    fun delete(): Either<ScopesError, ScopeAggregate> = either {
        val event = newEvent { aggregateId, eventId, occurredAt, version ->
            ScopeDeleted(
                aggregateId = aggregateId,
                eventId = eventId,
                occurredAt = occurredAt,
                version = version,
                scopeId = scopeId,
                deletedAt = occurredAt,
            )
        }.bind()

        applyChange(event).bind()
    }

    /**
     * Applies an event to update the aggregate state.
     * This is a pure function that returns a new aggregate instance.
     */
    override fun applyEvent(event: DomainEvent): Either<ScopesError, ScopeAggregate> = either {
        val newVersion = AggregateVersion.create(event.version).bind()

        when (event) {
            is ScopeCreated -> copy(
                version = newVersion,
                createdAt = event.occurredAt,
                updatedAt = event.occurredAt,
            )

            is ScopeTitleUpdated -> copy(
                version = newVersion,
                updatedAt = event.occurredAt,
                title = event.newTitle,
            )

            is ScopeDescriptionUpdated -> copy(
                version = newVersion,
                updatedAt = event.occurredAt,
                description = event.newDescription,
            )

            is ScopeParentChanged -> copy(
                version = newVersion,
                updatedAt = event.occurredAt,
                parentId = event.newParentId,
            )

            is ScopeAspectAdded -> copy(
                version = newVersion,
                updatedAt = event.occurredAt,
                aspects = aspects.add(event.aspectKey, event.aspectValue),
            )

            is ScopeAspectRemoved -> copy(
                version = newVersion,
                updatedAt = event.occurredAt,
                aspects = aspects.remove(event.aspectKey, event.aspectValue),
            )

            is ScopeAspectCleared -> copy(
                version = newVersion,
                updatedAt = event.occurredAt,
                aspects = aspects.remove(event.aspectKey),
            )

            is ScopeArchived -> copy(
                version = newVersion,
                updatedAt = event.occurredAt,
                isArchived = true,
                archivedAt = event.occurredAt,
            )

            is ScopeRestored -> copy(
                version = newVersion,
                updatedAt = event.occurredAt,
                isArchived = false,
                archivedAt = null,
            )

            is ScopeDeleted -> copy(
                version = newVersion,
                updatedAt = event.occurredAt,
            )

            else -> raise(
                AggregateConcurrencyError.InvalidEventSequence(
                    aggregateId = id,
                    expectedEventVersion = version.value + 1,
                    actualEventVersion = event.version,
                ),
            )
        }
    }

    /**
     * Converts the aggregate to the Scope entity.
     * This is useful for queries and read models.
     */
    fun toScope(): Scope = Scope(
        id = scopeId,
        title = title,
        description = description,
        parentId = parentId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        aspects = aspects,
    )
}
