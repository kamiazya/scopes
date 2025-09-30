package io.github.kamiazya.scopes.scopemanagement.domain.aggregate

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeArchived
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeAspectAdded
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeAspectRemoved
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeAspectsCleared
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeAspectsUpdated
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeCreated
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeDeleted
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeDescriptionUpdated
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeEvent
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeParentChanged
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeRestored
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeTitleUpdated
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jmolecules.ddd.types.AggregateRoot

/**
 * Scope aggregate root implementing event sourcing pattern with jMolecules DDD types.
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
 * - Implements jMolecules AggregateRoot<ScopeAggregate, ScopeId> for explicit DDD modeling
 */
data class ScopeAggregate(
    private val _id: ScopeId,
    @Deprecated("Use ScopeId directly instead of AggregateId", ReplaceWith("_id"))
    val aggregateId: AggregateId,
    val version: AggregateVersion,
    val createdAt: Instant,
    val updatedAt: Instant,
    val scope: Scope?,
    val isDeleted: Boolean = false,
    val isArchived: Boolean = false,
) : AggregateRoot<ScopeAggregate, ScopeId> {

    override fun getId(): ScopeId = _id

    // Event sourcing support - uncommitted events tracking
    private val uncommittedEventsList = mutableListOf<ScopeEvent>()
    val uncommittedEvents: List<ScopeEvent> get() = uncommittedEventsList.toList()

    /**
     * Raise a new domain event and apply it to update state.
     */
    protected fun raiseEvent(event: ScopeEvent): ScopeAggregate {
        uncommittedEventsList.add(event)
        return applyEvent(event)
    }

    /**
     * Mark all uncommitted events as committed.
     * Called after successful persistence.
     */
    fun markEventsAsCommitted() {
        uncommittedEventsList.clear()
    }

    /**
     * Get and clear uncommitted events atomically.
     */
    fun getAndClearUncommittedEvents(): List<ScopeEvent> {
        val events = uncommittedEventsList.toList()
        uncommittedEventsList.clear()
        return events
    }

    /**
     * Check if there are any uncommitted changes.
     */
    fun hasUncommittedChanges(): Boolean = uncommittedEventsList.isNotEmpty()

    /**
     * Replay events to rebuild aggregate state.
     */
    fun replayEvents(events: List<ScopeEvent>): ScopeAggregate {
        var aggregate = this
        for (event in events) {
            aggregate = aggregate.applyEvent(event)
        }
        return aggregate
    }

    companion object {
        /**
         * Creates a new scope aggregate for a create command.
         * Generates a ScopeCreated event after validation.
         */
        fun create(
            title: String,
            description: String? = null,
            parentId: ScopeId? = null,
            scopeId: ScopeId? = null,
            now: Instant = Clock.System.now(),
        ): Either<ScopesError, ScopeAggregate> = either {
            val validatedTitle = ScopeTitle.create(title).bind()
            val validatedDescription = ScopeDescription.create(description).bind()
            val scopeId = scopeId ?: ScopeId.generate()
            val aggregateId = scopeId.toAggregateId().bind()
            val eventId = EventId.generate()

            val event = ScopeCreated(
                aggregateId = aggregateId,
                eventId = eventId,
                occurredAt = now,
                aggregateVersion = AggregateVersion.initial().increment(),
                scopeId = scopeId,
                title = validatedTitle,
                description = validatedDescription,
                parentId = parentId,
            )

            val initialAggregate = ScopeAggregate(
                _id = scopeId,
                aggregateId = aggregateId,
                version = AggregateVersion.initial(),
                createdAt = now,
                updatedAt = now,
                scope = null,
                isDeleted = false,
                isArchived = false,
            )

            initialAggregate.raiseEvent(event)
        }

        /**
         * Creates an empty aggregate for event replay.
         * Used when loading an aggregate from the event store.
         */
        fun empty(scopeId: ScopeId, aggregateId: AggregateId): ScopeAggregate = ScopeAggregate(
            _id = scopeId,
            aggregateId = aggregateId,
            version = AggregateVersion.initial(),
            createdAt = Instant.DISTANT_PAST,
            updatedAt = Instant.DISTANT_PAST,
            scope = null,
            isDeleted = false,
            isArchived = false,
        )
    }

    /**
     * Updates the scope title after validation.
     * Ensures the scope exists and is not deleted.
     */
    fun updateTitle(title: String, now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        val currentScope = scope
        ensureNotNull(currentScope) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(currentScope.id)
        }

        val newTitle = ScopeTitle.create(title).bind()
        if (currentScope.title == newTitle) {
            return@either this@ScopeAggregate
        }

        val event = ScopeTitleUpdated(
            aggregateId = aggregateId,
            eventId = EventId.generate(),
            occurredAt = now,

            aggregateVersion = version.increment(),
            scopeId = currentScope.id,
            oldTitle = currentScope.title,
            newTitle = newTitle,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Updates the scope description after validation.
     */
    fun updateDescription(description: String?, now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        val currentScope = scope
        ensureNotNull(currentScope) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(currentScope.id)
        }

        val newDescription = ScopeDescription.create(description).bind()
        if (currentScope.description == newDescription) {
            return@either this@ScopeAggregate
        }

        val event = ScopeDescriptionUpdated(
            aggregateId = aggregateId,
            eventId = EventId.generate(),
            occurredAt = now,

            aggregateVersion = version.increment(),
            scopeId = currentScope.id,
            oldDescription = currentScope.description,
            newDescription = newDescription,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Changes the parent of the scope.
     * Validates hierarchy constraints before applying the change.
     */
    fun changeParent(newParentId: ScopeId?, now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        val currentScope = scope
        ensureNotNull(currentScope) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(currentScope.id)
        }

        if (currentScope.parentId == newParentId) {
            return@either this@ScopeAggregate
        }

        val event = ScopeParentChanged(
            aggregateId = aggregateId,
            eventId = EventId.generate(),
            occurredAt = now,

            aggregateVersion = version.increment(),
            scopeId = currentScope.id,
            oldParentId = currentScope.parentId,
            newParentId = newParentId,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Deletes the scope.
     * Soft delete that marks the scope as deleted.
     */
    fun delete(now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        val currentScope = scope
        ensureNotNull(currentScope) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(currentScope.id)
        }

        val event = ScopeDeleted(
            aggregateId = aggregateId,
            eventId = EventId.generate(),
            occurredAt = now,

            aggregateVersion = version.increment(),
            scopeId = currentScope.id,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Archives the scope.
     * Archived scopes are hidden but can be restored.
     */
    fun archive(now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        val currentScope = scope
        ensureNotNull(currentScope) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(currentScope.id)
        }
        ensure(!isArchived) {
            ScopeError.AlreadyArchived(currentScope.id)
        }

        val event = ScopeArchived(
            aggregateId = aggregateId,
            eventId = EventId.generate(),
            occurredAt = now,

            aggregateVersion = version.increment(),
            scopeId = currentScope.id,
            reason = null,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Restores an archived scope.
     */
    fun restore(now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        val currentScope = scope
        ensureNotNull(currentScope) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(currentScope.id)
        }
        ensure(isArchived) {
            ScopeError.NotArchived(currentScope.id)
        }

        val event = ScopeRestored(
            aggregateId = aggregateId,
            eventId = EventId.generate(),
            occurredAt = now,

            aggregateVersion = version.increment(),
            scopeId = currentScope.id,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Applies an event to update the aggregate state.
     * This is the core of the event sourcing pattern.
     *
     * Note: Each event application increments the version by 1.
     * We don't use event.aggregateVersion directly, but instead
     * increment based on the number of events applied.
     */
    fun applyEvent(event: ScopeEvent): ScopeAggregate = when (event) {
        is ScopeCreated -> copy(
            version = version.increment(),
            createdAt = event.occurredAt,
            updatedAt = event.occurredAt,
            scope = Scope(
                _id = event.scopeId,
                title = event.title,
                description = event.description,
                parentId = event.parentId,
                createdAt = event.occurredAt,
                updatedAt = event.occurredAt,
            ),
        )

        is ScopeTitleUpdated -> copy(
            version = version.increment(),
            updatedAt = event.occurredAt,
            scope = scope?.copy(
                title = event.newTitle,
                updatedAt = event.occurredAt,
            ),
        )

        is ScopeDescriptionUpdated -> copy(
            version = version.increment(),
            updatedAt = event.occurredAt,
            scope = scope?.copy(
                description = event.newDescription,
                updatedAt = event.occurredAt,
            ),
        )

        is ScopeParentChanged -> copy(
            version = version.increment(),
            updatedAt = event.occurredAt,
            scope = scope?.copy(
                parentId = event.newParentId,
                updatedAt = event.occurredAt,
            ),
        )

        is ScopeDeleted -> copy(
            version = version.increment(),
            updatedAt = event.occurredAt,
            isDeleted = true,
        )

        is ScopeArchived -> copy(
            version = version.increment(),
            updatedAt = event.occurredAt,
            isArchived = true,
        )

        is ScopeRestored -> copy(
            version = version.increment(),
            updatedAt = event.occurredAt,
            isArchived = false,
        )

        is ScopeAspectAdded,
        is ScopeAspectRemoved,
        is ScopeAspectsCleared,
        is ScopeAspectsUpdated,
        -> this@ScopeAggregate // Not implemented yet
    }

    fun validateVersion(expectedVersion: Long, now: Instant = Clock.System.now()): Either<ScopesError, Unit> = either {
        val versionValue = version.value
        if (versionValue.toLong() != expectedVersion) {
            raise(
                ScopeError.VersionMismatch(
                    scopeId = scope?.id ?: ScopeId.create(id.value.substringAfterLast("/")).bind(),
                    expectedVersion = expectedVersion,
                    actualVersion = versionValue.toLong(),
                ),
            )
        }
    }
}
