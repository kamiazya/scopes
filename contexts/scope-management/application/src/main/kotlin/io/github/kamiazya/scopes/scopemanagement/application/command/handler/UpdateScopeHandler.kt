package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.UpdateScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.UpdateScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.port.EventProjector
import io.github.kamiazya.scopes.scopemanagement.domain.repository.EventSourcingRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

private typealias PendingEventEnvelope = io.github.kamiazya.scopes.platform.domain.event.EventEnvelope.Pending<
    io.github.kamiazya.scopes.platform.domain.event.DomainEvent,
    >

/**
 * Handler for UpdateScope command using Event Sourcing pattern.
 *
 * This handler uses the event-sourced approach where:
 * - Updates are handled through ScopeAggregate methods
 * - All changes go through domain events
 * - EventSourcingRepository handles persistence
 * - No separate repositories needed
 */
class UpdateScopeHandler(
    private val eventSourcingRepository: EventSourcingRepository<DomainEvent>,
    private val eventProjector: EventProjector,
    private val transactionManager: TransactionManager,
    private val applicationErrorMapper: ApplicationErrorMapper,
    private val logger: Logger,
) : CommandHandler<UpdateScopeCommand, ScopeContractError, UpdateScopeResult> {

    override suspend operator fun invoke(command: UpdateScopeCommand): Either<ScopeContractError, UpdateScopeResult> = either {
        logger.info(
            "Updating scope using EventSourcing pattern",
            mapOf(
                "scopeId" to command.id,
                "hasTitle" to (command.title != null).toString(),
                "hasDescription" to (command.description != null).toString(),
            ),
        )

        transactionManager.inTransaction {
            either {
                // Parse scope ID
                val scopeId = ScopeId.create(command.id).mapLeft { idError ->
                    logger.warn("Invalid scope ID format", mapOf("scopeId" to command.id))
                    applicationErrorMapper.mapDomainError(
                        idError,
                        ErrorMappingContext(attemptedValue = command.id),
                    )
                }.bind()

                // Load current aggregate from events
                val aggregateId = scopeId.toAggregateId().mapLeft { error ->
                    applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                }.bind()

                val events = eventSourcingRepository.getEvents(aggregateId).mapLeft { error ->
                    applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                }.bind()

                // Reconstruct aggregate from events using fromEvents method
                val scopeEvents = events.filterIsInstance<io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeEvent>()
                val baseAggregate = io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAggregate.fromEvents(scopeEvents).mapLeft { error ->
                    applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                }.bind()

                if (baseAggregate == null) {
                    logger.warn("Scope not found", mapOf("scopeId" to command.id))
                    raise(
                        applicationErrorMapper.mapDomainError(
                            io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeError.NotFound(scopeId),
                            ErrorMappingContext(attemptedValue = command.id),
                        ),
                    )
                }

                // Apply updates through aggregate methods
                var currentAggregate = baseAggregate
                var eventsToSave = mutableListOf<PendingEventEnvelope>()

                // Apply title update if provided
                if (command.title != null) {
                    val titleUpdateResult = currentAggregate.handleUpdateTitle(command.title, Clock.System.now()).mapLeft { error ->
                        applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                    }.bind()

                    currentAggregate = titleUpdateResult.aggregate
                    eventsToSave.addAll(
                        titleUpdateResult.events.map { envelope ->
                            PendingEventEnvelope(envelope.event as io.github.kamiazya.scopes.platform.domain.event.DomainEvent)
                        },
                    )
                }

                // Apply description update if provided
                if (command.description != null) {
                    val descriptionUpdateResult = currentAggregate.handleUpdateDescription(command.description, Clock.System.now()).mapLeft { error ->
                        applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                    }.bind()

                    currentAggregate = descriptionUpdateResult.aggregate
                    eventsToSave.addAll(
                        descriptionUpdateResult.events.map { envelope ->
                            PendingEventEnvelope(envelope.event as io.github.kamiazya.scopes.platform.domain.event.DomainEvent)
                        },
                    )
                }

                // Persist events if any changes were made
                if (eventsToSave.isNotEmpty()) {
                    eventSourcingRepository.saveEventsWithVersioning(
                        aggregateId = currentAggregate.id,
                        events = eventsToSave,
                        expectedVersion = baseAggregate.version.value.toInt(),
                    ).mapLeft { error ->
                        applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                    }.bind()

                    // Project events to RDB in the same transaction
                    val domainEvents = eventsToSave.map { envelope -> envelope.event }
                    eventProjector.projectEvents(domainEvents).mapLeft { error ->
                        logger.error(
                            "Failed to project update events to RDB",
                            mapOf(
                                "error" to error.toString(),
                                "eventCount" to domainEvents.size.toString(),
                            ),
                        )
                        applicationErrorMapper.mapToContractError(error)
                    }.bind()
                }

                logger.info(
                    "Scope updated successfully using EventSourcing",
                    mapOf(
                        "scopeId" to command.id,
                        "hasChanges" to (eventsToSave.isNotEmpty()).toString(),
                        "eventsCount" to eventsToSave.size.toString(),
                    ),
                )

                // Extract scope data from aggregate for result mapping
                val scope = io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope(
                    id = currentAggregate.scopeId!!,
                    title = currentAggregate.title!!,
                    description = currentAggregate.description,
                    parentId = currentAggregate.parentId,
                    status = currentAggregate.status,
                    aspects = currentAggregate.aspects,
                    createdAt = currentAggregate.createdAt,
                    updatedAt = currentAggregate.updatedAt,
                )

                // Extract canonical alias from aggregate
                val canonicalAlias = currentAggregate.canonicalAliasId?.let { id ->
                    currentAggregate.aliases[id]?.aliasName?.value
                }

                val result = ScopeMapper.toUpdateScopeResult(scope, canonicalAlias)

                logger.info(
                    "Scope update workflow completed",
                    mapOf(
                        "scopeId" to scope.id.value,
                        "title" to scope.title.value,
                    ),
                )

                result
            }
        }.bind()
    }.onLeft { error ->
        logger.error(
            "Failed to update scope using EventSourcing",
            mapOf(
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                "message" to error.toString(),
            ),
        )
    }
}
