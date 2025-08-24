package io.github.kamiazya.scopes.scopemanagement.domain.aggregate

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.platform.domain.aggregate.AggregateRoot
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeArchived
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeCreated
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeDeleted
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeDescriptionUpdated
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeParentChanged
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeRestored
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeTitleUpdated
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
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
    val createdAt: Instant,
    val updatedAt: Instant,
    val scope: Scope?,
    val isDeleted: Boolean = false,
    val isArchived: Boolean = false,
) : AggregateRoot<ScopeAggregate, DomainEvent>() {

    companion object {
        /**
         * Creates a new scope aggregate for a create command.
         * Generates a ScopeCreated event after validation.
         */
        fun create(title: String, description: String? = null, parentId: ScopeId? = null): Either<ScopesError, ScopeAggregate> = either {
            val validatedTitle = ScopeTitle.create(title).bind()
            val validatedDescription = ScopeDescription.create(description).bind()
            val scopeId = ScopeId.generate()
            val aggregateId = scopeId.toAggregateId().bind()
            val eventId = EventId.generate()
            val now = Clock.System.now()

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

            initialAggregate.applyEvent(event)
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
    fun updateTitle(title: String): Either<ScopesError, ScopeAggregate> = either {
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
            occurredAt = Clock.System.now(),
            aggregateVersion = version.increment(),
            scopeId = scope!!.id,
            oldTitle = scope!!.title,
            newTitle = newTitle,
        )

        this@ScopeAggregate.copy(version = version.increment()).applyEvent(event)
    }

    /**
     * Updates the scope description after validation.
     */
    fun updateDescription(description: String?): Either<ScopesError, ScopeAggregate> = either {
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
            occurredAt = Clock.System.now(),
            aggregateVersion = version.increment(),
            scopeId = scope!!.id,
            oldDescription = scope!!.description,
            newDescription = newDescription,
        )

        this@ScopeAggregate.copy(version = version.increment()).applyEvent(event)
    }

    /**
     * Changes the parent of the scope.
     * Validates hierarchy constraints before applying the change.
     */
    fun changeParent(newParentId: ScopeId?): Either<ScopesError, ScopeAggregate> = either {
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
            occurredAt = Clock.System.now(),
            aggregateVersion = version.increment(),
            scopeId = scope!!.id,
            oldParentId = scope!!.parentId,
            newParentId = newParentId,
        )

        this@ScopeAggregate.copy(version = version.increment()).applyEvent(event)
    }

    /**
     * Deletes the scope.
     * Soft delete that marks the scope as deleted.
     */
    fun delete(): Either<ScopesError, ScopeAggregate> = either {
        ensureNotNull(scope) {
            ScopeError.NotFound(ScopeId.create(id.value.substringAfterLast("/")).bind())
        }
        ensure(!isDeleted) {
            ScopeError.AlreadyDeleted(scope!!.id)
        }

        val event = ScopeDeleted(
            aggregateId = id,
            eventId = EventId.generate(),
            occurredAt = Clock.System.now(),
            aggregateVersion = version.increment(),
            scopeId = scope!!.id,
        )

        this@ScopeAggregate.copy(version = version.increment()).applyEvent(event)
    }

    /**
     * Archives the scope.
     * Archived scopes are hidden but can be restored.
     */
    fun archive(): Either<ScopesError, ScopeAggregate> = either {
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
            occurredAt = Clock.System.now(),
            aggregateVersion = version.increment(),
            scopeId = scope!!.id,
            reason = null,
        )

        this@ScopeAggregate.copy(version = version.increment()).applyEvent(event)
    }

    /**
     * Restores an archived scope.
     */
    fun restore(): Either<ScopesError, ScopeAggregate> = either {
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
            occurredAt = Clock.System.now(),
            aggregateVersion = version.increment(),
            scopeId = scope!!.id,
        )

        this@ScopeAggregate.copy(version = version.increment()).applyEvent(event)
    }

    /**
     * Applies an event to update the aggregate state.
     * This is the core of the event sourcing pattern.
     */
    override fun applyEvent(event: DomainEvent): ScopeAggregate = when (event) {
        is ScopeCreated -> copy(
            version = event.aggregateVersion,
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
            version = event.aggregateVersion,
            updatedAt = event.occurredAt,
            scope = scope?.copy(
                title = event.newTitle,
                updatedAt = event.occurredAt,
            ),
        )

        is ScopeDescriptionUpdated -> copy(
            version = event.aggregateVersion,
            updatedAt = event.occurredAt,
            scope = scope?.copy(
                description = event.newDescription,
                updatedAt = event.occurredAt,
            ),
        )

        is ScopeParentChanged -> copy(
            version = event.aggregateVersion,
            updatedAt = event.occurredAt,
            scope = scope?.copy(
                parentId = event.newParentId,
                updatedAt = event.occurredAt,
            ),
        )

        is ScopeDeleted -> copy(
            version = event.aggregateVersion,
            updatedAt = event.occurredAt,
            isDeleted = true,
        )

        is ScopeArchived -> copy(
            version = event.aggregateVersion,
            updatedAt = event.occurredAt,
            isArchived = true,
        )

        is ScopeRestored -> copy(
            version = event.aggregateVersion,
            updatedAt = event.occurredAt,
            isArchived = false,
        )

        else -> this@ScopeAggregate
    }

    fun validateVersion(expectedVersion: Long): Either<ScopesError, Unit> = either {
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
