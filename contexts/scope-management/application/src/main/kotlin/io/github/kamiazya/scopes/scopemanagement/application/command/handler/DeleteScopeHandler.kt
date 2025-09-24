package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.DeleteScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.DeleteScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.application.port.EventPublisher
import io.github.kamiazya.scopes.scopemanagement.domain.repository.EventSourcingRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.hierarchy.ScopeHierarchyService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

/**
 * Handler for DeleteScope command using Event Sourcing pattern.
 *
 * This handler uses the event-sourced approach where:
 * - Deletion is handled through ScopeAggregate methods
 * - All changes go through domain events
 * - EventSourcingRepository handles persistence
 * - Soft delete that marks scope as deleted
 */
class DeleteScopeHandler(
    eventSourcingRepository: EventSourcingRepository<DomainEvent>,
    private val eventPublisher: EventPublisher,
    private val scopeRepository: ScopeRepository,
    private val scopeHierarchyService: ScopeHierarchyService,
    private val transactionManager: TransactionManager,
    applicationErrorMapper: ApplicationErrorMapper,
    logger: Logger,
) : AbstractEventSourcingHandler(eventSourcingRepository, applicationErrorMapper, logger),
    CommandHandler<DeleteScopeCommand, ScopeContractError, DeleteScopeResult> {

    override suspend operator fun invoke(command: DeleteScopeCommand): Either<ScopeContractError, DeleteScopeResult> = either {
        logger.info(
            "Deleting scope using EventSourcing pattern",
            mapOf(
                "scopeId" to command.id,
            ),
        )

        transactionManager.inTransaction {
            either {
                // Load existing aggregate using inherited method
                val baseAggregate = loadExistingAggregate(command.id).bind()

                // Get the scope ID for cascade operations
                val scopeId = baseAggregate.scopeId ?: error("Aggregate has no scope ID")

                // Handle cascade deletion if requested
                if (command.cascade) {
                    // If cascade is true, we need to delete all children recursively
                    deleteChildrenRecursively(scopeId).bind()
                } else {
                    // If cascade is false, validate that the scope has no children
                    val childCount = scopeRepository.countChildrenOf(scopeId).mapLeft { error ->
                        applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                    }.bind()

                    scopeHierarchyService.validateDeletion(scopeId, childCount).mapLeft { error ->
                        logger.warn(
                            "Cannot delete scope with children",
                            mapOf(
                                "scopeId" to command.id,
                                "childCount" to childCount.toString(),
                                "cascade" to command.cascade.toString(),
                            ),
                        )
                        applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                    }.bind()
                }

                // Apply delete through aggregate method
                val deleteResult = baseAggregate.handleDelete(Clock.System.now()).mapLeft { error ->
                    applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                }.bind()

                // Persist delete events
                val eventsToSave = deleteResult.events.map { envelope ->
                    io.github.kamiazya.scopes.platform.domain.event.EventEnvelope.Pending(envelope.event as DomainEvent)
                }

                eventSourcingRepository.saveEventsWithVersioning(
                    aggregateId = deleteResult.aggregate.id,
                    events = eventsToSave,
                    expectedVersion = baseAggregate.version.value.toInt(),
                ).mapLeft { error ->
                    applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                }.bind()

                // Project events to RDB in the same transaction
                val domainEvents = eventsToSave.map { envelope -> envelope.event }
                eventPublisher.projectEvents(domainEvents).mapLeft { error ->
                    logger.error(
                        "Failed to project delete events to RDB",
                        mapOf(
                            "error" to error.toString(),
                            "eventCount" to domainEvents.size.toString(),
                        ),
                    )
                    applicationErrorMapper.mapToContractError(error)
                }.bind()

                logger.info(
                    "Scope deleted successfully using EventSourcing",
                    mapOf(
                        "scopeId" to command.id,
                        "eventsCount" to eventsToSave.size.toString(),
                    ),
                )

                // Return success result
                DeleteScopeResult(
                    id = command.id,
                    deletedAt = Clock.System.now(),
                )
            }
        }.bind()
    }.onLeft { error ->
        logger.error(
            "Failed to delete scope using EventSourcing",
            mapOf(
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                "message" to error.toString(),
            ),
        )
    }

    /**
     * Recursively delete all children of a scope.
     * This is called when cascade=true to delete the entire hierarchy.
     */
    private suspend fun deleteChildrenRecursively(scopeId: ScopeId): Either<ScopeContractError, Unit> = either {
        // Get all direct children
        val children = scopeRepository.findByParentId(scopeId, offset = 0, limit = 1000).mapLeft { error ->
            applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
        }.bind()

        // Delete each child recursively
        children.forEach { childScope ->
            // First delete the child's children
            deleteChildrenRecursively(childScope.id).bind()

            // Then delete the child itself using the base class method
            val childAggregate = loadExistingAggregate(childScope.id.value).bind()

            // Apply delete to child aggregate
            val deleteResult = childAggregate.handleDelete(Clock.System.now()).mapLeft { error ->
                applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
            }.bind()

            // Persist delete events for child
            val eventsToSave = deleteResult.events.map { envelope ->
                io.github.kamiazya.scopes.platform.domain.event.EventEnvelope.Pending(envelope.event as DomainEvent)
            }

            eventSourcingRepository.saveEventsWithVersioning(
                aggregateId = deleteResult.aggregate.id,
                events = eventsToSave,
                expectedVersion = childAggregate.version.value.toInt(),
            ).mapLeft { error ->
                applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
            }.bind()

            // Project events to RDB
            val domainEvents = eventsToSave.map { envelope -> envelope.event }
            eventPublisher.projectEvents(domainEvents).mapLeft { error ->
                applicationErrorMapper.mapToContractError(error)
            }.bind()

            logger.debug(
                "Deleted child scope in cascade",
                mapOf(
                    "childScopeId" to childScope.id.value,
                    "parentScopeId" to scopeId.value,
                ),
            )
        }
    }
}
