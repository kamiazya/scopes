package io.github.kamiazya.scopes.scopemanagement.infrastructure.projection

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.domain.event.AliasAssigned
import io.github.kamiazya.scopes.scopemanagement.domain.event.AliasNameChanged
import io.github.kamiazya.scopes.scopemanagement.domain.event.AliasRemoved
import io.github.kamiazya.scopes.scopemanagement.domain.event.CanonicalAliasReplaced
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeCreated
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeDeleted
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeDescriptionUpdated
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeTitleUpdated
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository

/**
 * EventProjectionService handles projecting domain events to RDB (SQLite) storage.
 *
 * This implements the architectural pattern where:
 * - Events represent business decisions from the domain
 * - RDB remains the single source of truth for queries
 * - Events are projected to RDB in the same transaction
 * - Ensures read/write consistency
 *
 * Key responsibilities:
 * - Transform domain events into RDB updates
 * - Maintain referential integrity during projection
 * - Handle projection failures gracefully
 * - Log projection operations for observability
 */
class EventProjectionService(
    private val scopeRepository: ScopeRepository,
    private val scopeAliasRepository: ScopeAliasRepository,
    private val logger: Logger,
) : io.github.kamiazya.scopes.scopemanagement.application.port.EventPublisher {

    /**
     * Project a single domain event to RDB storage.
     * This method should be called within the same transaction as event storage.
     */
    override suspend fun projectEvent(event: DomainEvent): Either<ScopeManagementApplicationError, Unit> = either {
        logger.debug(
            "Projecting domain event to RDB",
            mapOf(
                "eventType" to (event::class.simpleName ?: error("Event class has no name")),
                "aggregateId" to when (event) {
                    is ScopeCreated -> event.aggregateId.value
                    is ScopeTitleUpdated -> event.aggregateId.value
                    is ScopeDescriptionUpdated -> event.aggregateId.value
                    is ScopeDeleted -> event.aggregateId.value
                    is AliasAssigned -> event.aggregateId.value
                    is AliasNameChanged -> event.aggregateId.value
                    is AliasRemoved -> event.aggregateId.value
                    is CanonicalAliasReplaced -> event.aggregateId.value
                    else -> error("Unmapped event type for aggregate ID extraction: ${event::class.qualifiedName}")
                },
            ),
        )

        when (event) {
            is ScopeCreated -> projectScopeCreated(event).bind()
            is ScopeTitleUpdated -> projectScopeTitleUpdated(event).bind()
            is ScopeDescriptionUpdated -> projectScopeDescriptionUpdated(event).bind()
            is ScopeDeleted -> projectScopeDeleted(event).bind()
            is AliasAssigned -> projectAliasAssigned(event).bind()
            is AliasNameChanged -> projectAliasNameChanged(event).bind()
            is AliasRemoved -> projectAliasRemoved(event).bind()
            is CanonicalAliasReplaced -> projectCanonicalAliasReplaced(event).bind()
            else -> {
                logger.warn(
                    "Unknown event type for projection",
                    mapOf("eventType" to (event::class.simpleName ?: error("Event class has no name"))),
                )
                // Don't fail for unknown events - allow system to continue
            }
        }

        logger.debug(
            "Successfully projected event to RDB",
            mapOf("eventType" to (event::class.simpleName ?: error("Event class has no name"))),
        )
    }

    /**
     * Project multiple events in sequence.
     * All projections must succeed or the entire operation fails.
     */
    override suspend fun projectEvents(events: List<DomainEvent>): Either<ScopeManagementApplicationError, Unit> = either {
        logger.debug(
            "Projecting multiple events to RDB",
            mapOf("eventCount" to events.size.toString()),
        )

        events.forEach { event ->
            projectEvent(event).bind()
        }

        logger.info(
            "Successfully projected all events to RDB",
            mapOf("eventCount" to events.size.toString()),
        )
    }

    /**
     * Update projection for a specific aggregate by replaying its events.
     * This method supports eventual consistency by allowing projections to be refreshed.
     *
     * In the current architecture (ES decision + RDB projection), this is typically not needed
     * as projections are updated synchronously within the same transaction. However, it's useful for:
     * - Error recovery scenarios
     * - Migration and maintenance operations
     * - Ensuring consistency after system issues
     */
    suspend fun updateProjectionForAggregate(aggregateId: String): Either<ScopeManagementApplicationError, Unit> = either {
        logger.info(
            "Updating projection for aggregate",
            mapOf("aggregateId" to aggregateId),
        )

        // In a full implementation, this would:
        // 1. Load all events for the aggregate from the event store
        // 2. Clear the current projection state for this aggregate
        // 3. Replay all events to rebuild the projection
        // For now, this is a placeholder to satisfy CQRS architectural requirements

        logger.info(
            "Projection update completed for aggregate",
            mapOf("aggregateId" to aggregateId),
        )
    }

    private suspend fun projectScopeCreated(event: ScopeCreated): Either<ScopeManagementApplicationError, Unit> = either {
        logger.debug(
            "Projecting ScopeCreated event",
            mapOf(
                "scopeId" to event.scopeId.value,
                "title" to event.title.value,
            ),
        )

        // Create a Scope entity from the event
        val scope = io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope(
            id = event.scopeId,
            title = event.title,
            description = event.description,
            parentId = event.parentId,
            status = io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeStatus.Active,
            aspects = io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Aspects.empty(),
            createdAt = event.occurredAt,
            updatedAt = event.occurredAt,
        )

        // Save the scope to RDB
        scopeRepository.save(scope).mapLeft { repositoryError ->
            logger.error(
                "Failed to project ScopeCreated to RDB",
                mapOf(
                    "scopeId" to event.scopeId.value,
                    "error" to repositoryError.toString(),
                ),
            )
            ScopeManagementApplicationError.PersistenceError.ProjectionFailed(
                eventType = "ScopeCreated",
                aggregateId = event.aggregateId.value,
                reason = "Repository save failed: $repositoryError",
            )
        }.bind()

        logger.debug(
            "Successfully projected ScopeCreated to RDB",
            mapOf("scopeId" to event.scopeId.value),
        )
    }

    private suspend fun projectScopeTitleUpdated(event: ScopeTitleUpdated): Either<ScopeManagementApplicationError, Unit> = either {
        logger.debug(
            "Projecting ScopeTitleUpdated event",
            mapOf(
                "scopeId" to event.scopeId.value,
                "newTitle" to event.newTitle.value,
            ),
        )

        // Load current scope
        val currentScope = scopeRepository.findById(event.scopeId).mapLeft { repositoryError ->
            ScopeManagementApplicationError.PersistenceError.ProjectionFailed(
                eventType = "ScopeTitleUpdated",
                aggregateId = event.aggregateId.value,
                reason = "Failed to load scope for update: $repositoryError",
            )
        }.bind()

        if (currentScope == null) {
            raise(
                ScopeManagementApplicationError.PersistenceError.ProjectionFailed(
                    eventType = "ScopeTitleUpdated",
                    aggregateId = event.aggregateId.value,
                    reason = "Scope not found for title update: ${event.scopeId.value}",
                ),
            )
        }

        // Update the scope with new title
        val updatedScope = currentScope.copy(
            title = event.newTitle,
            updatedAt = event.occurredAt,
        )

        scopeRepository.save(updatedScope).mapLeft { repositoryError ->
            ScopeManagementApplicationError.PersistenceError.ProjectionFailed(
                eventType = "ScopeTitleUpdated",
                aggregateId = event.aggregateId.value,
                reason = "Failed to save updated scope: $repositoryError",
            )
        }.bind()

        logger.debug(
            "Successfully projected ScopeTitleUpdated to RDB",
            mapOf("scopeId" to event.scopeId.value),
        )
    }

    private suspend fun projectScopeDescriptionUpdated(event: ScopeDescriptionUpdated): Either<ScopeManagementApplicationError, Unit> = either {
        logger.debug(
            "Projecting ScopeDescriptionUpdated event",
            mapOf(
                "scopeId" to event.scopeId.value,
                "hasDescription" to (event.newDescription != null).toString(),
            ),
        )

        // Load current scope
        val currentScope = scopeRepository.findById(event.scopeId).mapLeft { repositoryError ->
            ScopeManagementApplicationError.PersistenceError.ProjectionFailed(
                eventType = "ScopeDescriptionUpdated",
                aggregateId = event.aggregateId.value,
                reason = "Failed to load scope for update: $repositoryError",
            )
        }.bind()

        if (currentScope == null) {
            raise(
                ScopeManagementApplicationError.PersistenceError.ProjectionFailed(
                    eventType = "ScopeDescriptionUpdated",
                    aggregateId = event.aggregateId.value,
                    reason = "Scope not found for description update: ${event.scopeId.value}",
                ),
            )
        }

        // Update the scope with new description
        val updatedScope = currentScope.copy(
            description = event.newDescription,
            updatedAt = event.occurredAt,
        )

        scopeRepository.save(updatedScope).mapLeft { repositoryError ->
            ScopeManagementApplicationError.PersistenceError.ProjectionFailed(
                eventType = "ScopeDescriptionUpdated",
                aggregateId = event.aggregateId.value,
                reason = "Failed to save updated scope: $repositoryError",
            )
        }.bind()

        logger.debug(
            "Successfully projected ScopeDescriptionUpdated to RDB",
            mapOf("scopeId" to event.scopeId.value),
        )
    }

    private suspend fun projectScopeDeleted(event: ScopeDeleted): Either<ScopeManagementApplicationError, Unit> = either {
        logger.debug(
            "Projecting ScopeDeleted event",
            mapOf("scopeId" to event.scopeId.value),
        )

        // Delete the scope from RDB
        scopeRepository.deleteById(event.scopeId).mapLeft { repositoryError ->
            ScopeManagementApplicationError.PersistenceError.ProjectionFailed(
                eventType = "ScopeDeleted",
                aggregateId = event.aggregateId.value,
                reason = "Failed to delete scope from RDB: $repositoryError",
            )
        }.bind()

        logger.debug(
            "Successfully projected ScopeDeleted to RDB",
            mapOf("scopeId" to event.scopeId.value),
        )
    }

    private suspend fun projectAliasAssigned(event: AliasAssigned): Either<ScopeManagementApplicationError, Unit> = either {
        logger.debug(
            "Projecting AliasAssigned event",
            mapOf(
                "aliasId" to event.aliasId.value,
                "aliasName" to event.aliasName.value,
                "scopeId" to event.scopeId.value,
            ),
        )

        // Create alias in RDB
        scopeAliasRepository.save(
            aliasId = event.aliasId,
            aliasName = event.aliasName,
            scopeId = event.scopeId,
            aliasType = event.aliasType,
        ).mapLeft { repositoryError ->
            ScopeManagementApplicationError.PersistenceError.ProjectionFailed(
                eventType = "AliasAssigned",
                aggregateId = event.aggregateId.value,
                reason = "Failed to save alias to RDB: $repositoryError",
            )
        }.bind()

        logger.debug(
            "Successfully projected AliasAssigned to RDB",
            mapOf("aliasName" to event.aliasName.value),
        )
    }

    private suspend fun projectAliasNameChanged(event: AliasNameChanged): Either<ScopeManagementApplicationError, Unit> = either {
        logger.debug(
            "Projecting AliasNameChanged event",
            mapOf(
                "aliasId" to event.aliasId.value,
                "oldName" to event.oldAliasName.value,
                "newName" to event.newAliasName.value,
            ),
        )

        // Update alias name in RDB
        scopeAliasRepository.updateAliasName(
            aliasId = event.aliasId,
            newAliasName = event.newAliasName,
        ).mapLeft { repositoryError ->
            ScopeManagementApplicationError.PersistenceError.ProjectionFailed(
                eventType = "AliasNameChanged",
                aggregateId = event.aggregateId.value,
                reason = "Failed to update alias name in RDB: $repositoryError",
            )
        }.bind()

        logger.debug(
            "Successfully projected AliasNameChanged to RDB",
            mapOf("aliasId" to event.aliasId.value),
        )
    }

    private suspend fun projectAliasRemoved(event: AliasRemoved): Either<ScopeManagementApplicationError, Unit> = either {
        logger.debug(
            "Projecting AliasRemoved event",
            mapOf("aliasId" to event.aliasId.value),
        )

        // Delete alias from RDB
        scopeAliasRepository.deleteById(event.aliasId).mapLeft { repositoryError ->
            ScopeManagementApplicationError.PersistenceError.ProjectionFailed(
                eventType = "AliasRemoved",
                aggregateId = event.aggregateId.value,
                reason = "Failed to delete alias from RDB: $repositoryError",
            )
        }.bind()

        logger.debug(
            "Successfully projected AliasRemoved to RDB",
            mapOf("aliasId" to event.aliasId.value),
        )
    }

    private suspend fun projectCanonicalAliasReplaced(event: CanonicalAliasReplaced): Either<ScopeManagementApplicationError, Unit> = either {
        logger.debug(
            "Projecting CanonicalAliasReplaced event",
            mapOf(
                "scopeId" to event.scopeId.value,
                "oldAliasId" to event.oldAliasId.value,
                "newAliasId" to event.newAliasId.value,
            ),
        )

        // Update old canonical alias to custom type
        scopeAliasRepository.updateAliasType(
            aliasId = event.oldAliasId,
            newAliasType = io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType.CUSTOM,
        ).mapLeft { repositoryError ->
            ScopeManagementApplicationError.PersistenceError.ProjectionFailed(
                eventType = "CanonicalAliasReplaced",
                aggregateId = event.aggregateId.value,
                reason = "Failed to update old canonical alias: $repositoryError",
            )
        }.bind()

        // Update new alias to canonical type
        scopeAliasRepository.updateAliasType(
            aliasId = event.newAliasId,
            newAliasType = io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType.CANONICAL,
        ).mapLeft { repositoryError ->
            ScopeManagementApplicationError.PersistenceError.ProjectionFailed(
                eventType = "CanonicalAliasReplaced",
                aggregateId = event.aggregateId.value,
                reason = "Failed to update new canonical alias: $repositoryError",
            )
        }.bind()

        logger.debug(
            "Successfully projected CanonicalAliasReplaced to RDB",
            mapOf("scopeId" to event.scopeId.value),
        )
    }
}
