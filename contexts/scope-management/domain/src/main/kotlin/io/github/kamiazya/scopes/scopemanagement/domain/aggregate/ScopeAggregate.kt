package io.github.kamiazya.scopes.scopemanagement.domain.aggregate

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.platform.domain.aggregate.AggregateResult
import io.github.kamiazya.scopes.platform.domain.aggregate.AggregateRoot
import kotlinx.datetime.Clock
import io.github.kamiazya.scopes.platform.domain.event.EventEnvelope
import io.github.kamiazya.scopes.platform.domain.event.evolveWithPending
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
    val createdAt: Instant,
    val updatedAt: Instant,
    val scope: Scope?,
    val isDeleted: Boolean = false,
    val isArchived: Boolean = false,
) : AggregateRoot<ScopeAggregate, ScopeEvent>() {

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
                id = aggregateId,
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
         * Creates a scope using decide/evolve pattern.
         * Returns an AggregateResult with the new aggregate and pending events.
         */
        fun handleCreate(
            title: String,
            description: String? = null,
            parentId: ScopeId? = null,
            scopeId: ScopeId? = null,
            now: Instant = Clock.System.now(),
        ): Either<ScopesError, AggregateResult<ScopeAggregate, ScopeEvent>> = either {
            val validatedTitle = ScopeTitle.create(title).bind()
            val validatedDescription = ScopeDescription.create(description).bind()
            val scopeId = scopeId ?: ScopeId.generate()
            val aggregateId = scopeId.toAggregateId().bind()

            val initialAggregate = ScopeAggregate(
                id = aggregateId,
                version = AggregateVersion.initial(),
                createdAt = now,
                updatedAt = now,
                scope = null,
                isDeleted = false,
                isArchived = false,
            )

            // Decide phase - create events with dummy version
            val event = ScopeCreated(
                aggregateId = aggregateId,
                eventId = EventId.generate(),
                occurredAt = now,

                aggregateVersion = AggregateVersion.initial(), // Dummy version
                scopeId = scopeId,
                title = validatedTitle,
                description = validatedDescription,
                parentId = parentId,
            )

            val pendingEvents = listOf(EventEnvelope.Pending(event))

            // Evolve phase - apply events to aggregate
            val evolvedAggregate = initialAggregate.evolveWithPending(pendingEvents)

            AggregateResult(
                aggregate = evolvedAggregate,
                events = pendingEvents,
                baseVersion = AggregateVersion.initial(),
            )
        }

        /**
         * Creates an empty aggregate for event replay.
         * Used when loading an aggregate from the event store.
         */
        fun empty(aggregateId: AggregateId): ScopeAggregate = ScopeAggregate(
            id = aggregateId,
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
        ensureNotNull(scope) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scope!!.id)
        }

        val newTitle = ScopeTitle.create(title).bind()
        if (scope!!.title == newTitle) {
            return@either this@ScopeAggregate
        }

        val event = ScopeTitleUpdated(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,

            aggregateVersion = version.increment(),
            scopeId = scope!!.id,
            oldTitle = scope!!.title,
            newTitle = newTitle,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Decides whether to update the title (decide phase).
     * Returns pending events or empty list if no change needed.
     */
    fun decideUpdateTitle(title: String, now: Instant = Clock.System.now()): Either<ScopesError, List<EventEnvelope.Pending<ScopeEvent>>> = either {
        ensureNotNull(scope) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scope!!.id)
        }

        val newTitle = ScopeTitle.create(title).bind()
        if (scope!!.title == newTitle) {
            return@either emptyList()
        }

        val event = ScopeTitleUpdated(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,

            aggregateVersion = AggregateVersion.initial(), // Dummy version
            scopeId = scope!!.id,
            oldTitle = scope!!.title,
            newTitle = newTitle,
        )

        listOf(EventEnvelope.Pending(event))
    }

    /**
     * Handles update title command using decide/evolve pattern.
     * Returns an AggregateResult with the updated aggregate and pending events.
     */
    fun handleUpdateTitle(title: String, now: Instant = Clock.System.now()): Either<ScopesError, AggregateResult<ScopeAggregate, ScopeEvent>> = either {
        val pendingEvents = decideUpdateTitle(title, now).bind()

        if (pendingEvents.isEmpty()) {
            return@either AggregateResult(
                aggregate = this@ScopeAggregate,
                events = emptyList(),
                baseVersion = version,
            )
        }

        // Evolve phase - apply events to aggregate
        val evolvedAggregate = pendingEvents.fold(this@ScopeAggregate) { agg, envelope ->
            agg.applyEvent(envelope.event)
        }

        AggregateResult(
            aggregate = evolvedAggregate,
            events = pendingEvents,
            baseVersion = version,
        )
    }

    /**
     * Updates the scope description after validation.
     */
    fun updateDescription(description: String?, now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        ensureNotNull(scope) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scope!!.id)
        }

        val newDescription = ScopeDescription.create(description).bind()
        if (scope!!.description == newDescription) {
            return@either this@ScopeAggregate
        }

        val event = ScopeDescriptionUpdated(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,

            aggregateVersion = version.increment(),
            scopeId = scope!!.id,
            oldDescription = scope!!.description,
            newDescription = newDescription,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Changes the parent of the scope.
     * Validates hierarchy constraints before applying the change.
     */
    fun changeParent(newParentId: ScopeId?, now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        ensureNotNull(scope) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scope!!.id)
        }

        if (scope!!.parentId == newParentId) {
            return@either this@ScopeAggregate
        }

        val event = ScopeParentChanged(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,

            aggregateVersion = version.increment(),
            scopeId = scope!!.id,
            oldParentId = scope!!.parentId,
            newParentId = newParentId,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Deletes the scope.
     * Soft delete that marks the scope as deleted.
     */
    fun delete(now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        ensureNotNull(scope) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scope!!.id)
        }

        val event = ScopeDeleted(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,

            aggregateVersion = version.increment(),
            scopeId = scope!!.id,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Archives the scope.
     * Archived scopes are hidden but can be restored.
     */
    fun archive(now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        ensureNotNull(scope) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scope!!.id)
        }
        ensure(!isArchived) {
            ScopeError.AlreadyArchived(scope!!.id)
        }

        val event = ScopeArchived(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,

            aggregateVersion = version.increment(),
            scopeId = scope!!.id,
            reason = null,
        )

        this@ScopeAggregate.raiseEvent(event)
    }

    /**
     * Restores an archived scope.
     */
    fun restore(now: Instant = Clock.System.now()): Either<ScopesError, ScopeAggregate> = either {
        ensureNotNull(scope) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scope!!.id)
        }
        ensure(isArchived) {
            ScopeError.NotArchived(scope!!.id)
        }

        val event = ScopeRestored(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = now,

            aggregateVersion = version.increment(),
            scopeId = scope!!.id,
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
    override fun applyEvent(event: ScopeEvent): ScopeAggregate = when (event) {
        is ScopeCreated -> copy(
            version = version.increment(),
            createdAt = event.occurredAt,
            updatedAt = event.occurredAt,
            scope = Scope(
                id = event.scopeId,
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
