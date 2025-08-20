package io.github.kamiazya.scopes.domain.service

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.domain.aggregate.ScopeAggregate
import io.github.kamiazya.scopes.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.domain.error.ScopeNotFoundError
import io.github.kamiazya.scopes.domain.error.ScopesError
import io.github.kamiazya.scopes.domain.repository.EventSourcingRepository
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.valueobject.AggregateVersion
import io.github.kamiazya.scopes.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Command handler for Scope aggregate operations.
 *
 * This service acts as the application layer between the domain and infrastructure,
 * coordinating the execution of commands against Scope aggregates. It handles:
 * - Loading aggregates from the event store
 * - Executing commands to generate events
 * - Persisting events with concurrency control
 * - Publishing events to other parts of the system
 *
 * Design principles:
 * - Commands are the write-side operations in CQRS
 * - Each command method represents a user intention
 * - Commands validate business rules and generate events
 * - Optimistic concurrency control prevents lost updates
 *
 * @param eventRepository Repository for event persistence
 * @param scopeRepository Repository for read model (optional, for queries)
 * @param eventPublisher Service to publish events to other bounded contexts
 */
class ScopeCommandHandler(
    private val eventRepository: EventSourcingRepository<ScopeAggregate>,
    private val scopeRepository: ScopeRepository? = null,
    private val eventPublisher: DomainEventPublisher? = null,
) {

    /**
     * Creates a new Scope.
     *
     * @param title The title of the scope
     * @param description Optional description
     * @param parentId Optional parent scope ID
     * @param aspects Initial aspects to set
     * @return Either an error or the created scope ID
     */
    suspend fun createScope(
        title: String,
        description: String? = null,
        parentId: ScopeId? = null,
        aspects: Map<AspectKey, NonEmptyList<AspectValue>> = emptyMap(),
    ): Either<ScopesError, ScopeId> = either {
        // Validate parent exists if specified
        if (parentId != null && scopeRepository != null) {
            val parentExists = scopeRepository.existsById(parentId).bind()
            ensure(parentExists) {
                ScopeHierarchyError.ParentNotFound(
                    occurredAt = kotlinx.datetime.Clock.System.now(),
                    scopeId = ScopeId.generate(), // Temporary ID for error
                    parentId = parentId,
                )
            }
        }

        // Create the aggregate
        val aggregate = ScopeAggregate.create(
            title = title,
            description = description,
            parentId = parentId,
            aspectsData = aspects,
        ).bind()

        // Save events
        val events = aggregate.getUncommittedEvents()
        eventRepository.saveEvents(
            aggregateId = aggregate.id,
            events = events,
            expectedVersion = 0,
        ).bind()

        // Publish events
        eventPublisher?.publishAll(events)

        // Update read model if available
        scopeRepository?.save(aggregate.toScope())?.bind()

        aggregate.scopeId
    }

    /**
     * Updates the title of an existing scope.
     *
     * @param scopeId The ID of the scope to update
     * @param newTitle The new title
     * @param expectedVersion Expected version for optimistic locking
     * @return Either an error or Unit on success
     */
    suspend fun updateTitle(
        scopeId: ScopeId,
        newTitle: String,
        expectedVersion: AggregateVersion,
    ): Either<ScopesError, Unit> = either {
        val aggregate = loadAggregate(scopeId).bind()

        // Validate version
        aggregate.validateVersion(expectedVersion).bind()

        // Execute command
        val updatedAggregate = aggregate.updateTitle(newTitle).bind()

        // Save new events
        val events = updatedAggregate.getUncommittedEvents()
        if (events.isNotEmpty()) {
            eventRepository.saveEvents(
                aggregateId = updatedAggregate.id,
                events = events,
                expectedVersion = expectedVersion.value,
            ).bind()

            // Publish events
            eventPublisher?.publishAll(events)

            // Update read model
            scopeRepository?.update(updatedAggregate.toScope())?.bind()
        }
    }

    /**
     * Updates the description of an existing scope.
     *
     * @param scopeId The ID of the scope to update
     * @param newDescription The new description (null to remove)
     * @param expectedVersion Expected version for optimistic locking
     * @return Either an error or Unit on success
     */
    suspend fun updateDescription(
        scopeId: ScopeId,
        newDescription: String?,
        expectedVersion: AggregateVersion,
    ): Either<ScopesError, Unit> = either {
        val aggregate = loadAggregate(scopeId).bind()
        aggregate.validateVersion(expectedVersion).bind()

        val updatedAggregate = aggregate.updateDescription(newDescription).bind()

        val events = updatedAggregate.getUncommittedEvents()
        if (events.isNotEmpty()) {
            eventRepository.saveEvents(
                aggregateId = updatedAggregate.id,
                events = events,
                expectedVersion = expectedVersion.value,
            ).bind()

            eventPublisher?.publishAll(events)
            scopeRepository?.update(updatedAggregate.toScope())?.bind()
        }
    }

    /**
     * Changes the parent of a scope.
     *
     * @param scopeId The ID of the scope to move
     * @param newParentId The new parent ID (null to make it root)
     * @param expectedVersion Expected version for optimistic locking
     * @return Either an error or Unit on success
     */
    suspend fun changeParent(
        scopeId: ScopeId,
        newParentId: ScopeId?,
        expectedVersion: AggregateVersion,
    ): Either<ScopesError, Unit> = either {
        // Validate new parent exists if specified
        if (newParentId != null && scopeRepository != null) {
            val parentExists = scopeRepository.existsById(newParentId).bind()
            ensure(parentExists) {
                ScopeHierarchyError.ParentNotFound(
                    occurredAt = kotlinx.datetime.Clock.System.now(),
                    scopeId = scopeId,
                    parentId = newParentId,
                )
            }

            // TODO: Add circular reference detection
        }

        val aggregate = loadAggregate(scopeId).bind()
        aggregate.validateVersion(expectedVersion).bind()

        val updatedAggregate = aggregate.changeParent(newParentId).bind()

        val events = updatedAggregate.getUncommittedEvents()
        if (events.isNotEmpty()) {
            eventRepository.saveEvents(
                aggregateId = updatedAggregate.id,
                events = events,
                expectedVersion = expectedVersion.value,
            ).bind()

            eventPublisher?.publishAll(events)
            scopeRepository?.update(updatedAggregate.toScope())?.bind()
        }
    }

    /**
     * Adds an aspect to a scope.
     *
     * @param scopeId The ID of the scope
     * @param aspectKey The aspect key
     * @param aspectValue The aspect value
     * @param expectedVersion Expected version for optimistic locking
     * @return Either an error or Unit on success
     */
    suspend fun addAspect(
        scopeId: ScopeId,
        aspectKey: AspectKey,
        aspectValue: AspectValue,
        expectedVersion: AggregateVersion,
    ): Either<ScopesError, Unit> = either {
        val aggregate = loadAggregate(scopeId).bind()
        aggregate.validateVersion(expectedVersion).bind()

        val updatedAggregate = aggregate.addAspect(aspectKey, aspectValue).bind()

        val events = updatedAggregate.getUncommittedEvents()
        if (events.isNotEmpty()) {
            eventRepository.saveEvents(
                aggregateId = updatedAggregate.id,
                events = events,
                expectedVersion = expectedVersion.value,
            ).bind()

            eventPublisher?.publishAll(events)
            scopeRepository?.update(updatedAggregate.toScope())?.bind()
        }
    }

    /**
     * Removes an aspect from a scope.
     *
     * @param scopeId The ID of the scope
     * @param aspectKey The aspect key
     * @param aspectValue The aspect value to remove
     * @param expectedVersion Expected version for optimistic locking
     * @return Either an error or Unit on success
     */
    suspend fun removeAspect(
        scopeId: ScopeId,
        aspectKey: AspectKey,
        aspectValue: AspectValue,
        expectedVersion: AggregateVersion,
    ): Either<ScopesError, Unit> = either {
        val aggregate = loadAggregate(scopeId).bind()
        aggregate.validateVersion(expectedVersion).bind()

        val updatedAggregate = aggregate.removeAspect(aspectKey, aspectValue).bind()

        val events = updatedAggregate.getUncommittedEvents()
        if (events.isNotEmpty()) {
            eventRepository.saveEvents(
                aggregateId = updatedAggregate.id,
                events = events,
                expectedVersion = expectedVersion.value,
            ).bind()

            eventPublisher?.publishAll(events)
            scopeRepository?.update(updatedAggregate.toScope())?.bind()
        }
    }

    /**
     * Archives a scope (soft delete).
     *
     * @param scopeId The ID of the scope to archive
     * @param reason Optional reason for archiving
     * @param expectedVersion Expected version for optimistic locking
     * @return Either an error or Unit on success
     */
    suspend fun archiveScope(
        scopeId: ScopeId,
        reason: String? = null,
        expectedVersion: AggregateVersion,
    ): Either<ScopesError, Unit> = either {
        val aggregate = loadAggregate(scopeId).bind()
        aggregate.validateVersion(expectedVersion).bind()

        val updatedAggregate = aggregate.archive(reason).bind()

        val events = updatedAggregate.getUncommittedEvents()
        eventRepository.saveEvents(
            aggregateId = updatedAggregate.id,
            events = events,
            expectedVersion = expectedVersion.value,
        ).bind()

        eventPublisher?.publishAll(events)

        // Note: Read model might handle archived scopes differently
        scopeRepository?.update(updatedAggregate.toScope())?.bind()
    }

    /**
     * Restores an archived scope.
     *
     * @param scopeId The ID of the scope to restore
     * @param expectedVersion Expected version for optimistic locking
     * @return Either an error or Unit on success
     */
    suspend fun restoreScope(scopeId: ScopeId, expectedVersion: AggregateVersion): Either<ScopesError, Unit> = either {
        val aggregate = loadAggregate(scopeId).bind()
        aggregate.validateVersion(expectedVersion).bind()

        val updatedAggregate = aggregate.restore().bind()

        val events = updatedAggregate.getUncommittedEvents()
        eventRepository.saveEvents(
            aggregateId = updatedAggregate.id,
            events = events,
            expectedVersion = expectedVersion.value,
        ).bind()

        eventPublisher?.publishAll(events)
        scopeRepository?.update(updatedAggregate.toScope())?.bind()
    }

    /**
     * Permanently deletes a scope.
     *
     * @param scopeId The ID of the scope to delete
     * @param expectedVersion Expected version for optimistic locking
     * @return Either an error or Unit on success
     */
    suspend fun deleteScope(scopeId: ScopeId, expectedVersion: AggregateVersion): Either<ScopesError, Unit> = either {
        val aggregate = loadAggregate(scopeId).bind()
        aggregate.validateVersion(expectedVersion).bind()

        val updatedAggregate = aggregate.delete().bind()

        val events = updatedAggregate.getUncommittedEvents()
        eventRepository.saveEvents(
            aggregateId = updatedAggregate.id,
            events = events,
            expectedVersion = expectedVersion.value,
        ).bind()

        eventPublisher?.publishAll(events)

        // Remove from read model
        scopeRepository?.deleteById(scopeId)?.bind()
    }

    /**
     * Loads an aggregate from the event store.
     *
     * @param scopeId The ID of the scope to load
     * @return Either an error or the loaded aggregate
     */
    private suspend fun loadAggregate(scopeId: ScopeId): Either<ScopesError, ScopeAggregate> = either {
        val aggregateId = scopeId.toAggregateId().bind()
        val events = eventRepository.getEvents(aggregateId).bind()

        ensureNotNull(events.isNotEmpty()) {
            ScopeNotFoundError(
                occurredAt = kotlinx.datetime.Clock.System.now(),
                scopeId = scopeId,
            )
        }

        ScopeAggregate.fromEvents(events).bind()
    }
}
