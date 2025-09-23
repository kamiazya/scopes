package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.port.HierarchyPolicyProvider
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeHierarchyApplicationService
import io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAggregate
import io.github.kamiazya.scopes.scopemanagement.domain.extensions.persistScopeAggregate
import io.github.kamiazya.scopes.scopemanagement.domain.repository.EventSourcingRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.hierarchy.ScopeHierarchyService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import io.github.kamiazya.scopes.scopemanagement.application.port.EventProjector
import kotlinx.datetime.Clock

/**
 * Handler for CreateScope command using Event Sourcing pattern.
 *
 * This handler uses the event-sourced approach where:
 * - Scope and alias management is unified in ScopeAggregate
 * - All changes go through domain events
 * - EventSourcingRepository handles persistence
 * - No separate ScopeAliasRepository needed
 * - Alias generation is handled internally by ScopeAggregate
 * - Full business rule validation
 */
class CreateScopeHandler(
    private val eventSourcingRepository: EventSourcingRepository<DomainEvent>,
    private val scopeRepository: ScopeRepository,
    private val hierarchyApplicationService: ScopeHierarchyApplicationService,
    private val hierarchyService: ScopeHierarchyService,
    private val transactionManager: TransactionManager,
    private val hierarchyPolicyProvider: HierarchyPolicyProvider,
    private val eventProjector: EventProjector,
    private val applicationErrorMapper: ApplicationErrorMapper,
    private val logger: Logger,
) : CommandHandler<CreateScopeCommand, ScopeContractError, CreateScopeResult> {

    override suspend operator fun invoke(command: CreateScopeCommand): Either<ScopeContractError, CreateScopeResult> = either {
        val aliasStrategy = when (command) {
            is CreateScopeCommand.WithAutoAlias -> "auto"
            is CreateScopeCommand.WithCustomAlias -> "custom"
        }

        logger.info(
            "Creating new scope using EventSourcing pattern",
            mapOf(
                "title" to command.title,
                "parentId" to (command.parentId ?: "none"),
                "aliasStrategy" to aliasStrategy,
            ),
        )

        // Get hierarchy policy from external context
        val hierarchyPolicy = hierarchyPolicyProvider.getPolicy()
            .mapLeft { error -> applicationErrorMapper.mapDomainError(error, ErrorMappingContext()) }
            .bind()

        transactionManager.inTransaction {
            either {
                // Parse parent ID if provided
                val parentId = command.parentId?.let { parentIdString ->
                    ScopeId.create(parentIdString).mapLeft { idError ->
                        logger.warn("Invalid parent ID format", mapOf("parentId" to parentIdString))
                        applicationErrorMapper.mapDomainError(
                            idError,
                            ErrorMappingContext(attemptedValue = parentIdString),
                        )
                    }.bind()
                }

                // Validate title format early
                val validatedTitle = ScopeTitle.create(command.title)
                    .mapLeft { titleError ->
                        applicationErrorMapper.mapDomainError(
                            titleError,
                            ErrorMappingContext(attemptedValue = command.title),
                        )
                    }.bind()

                // Generate the scope ID early for error messages
                val newScopeId = ScopeId.generate()

                // Validate hierarchy constraints if parent is specified
                if (parentId != null) {
                    // Validate parent exists
                    val parentExists = scopeRepository.existsById(parentId)
                        .mapLeft { error ->
                            applicationErrorMapper.mapDomainError(
                                error,
                                ErrorMappingContext(),
                            )
                        }.bind()

                    ensure(parentExists) {
                        applicationErrorMapper.mapToContractError(
                            io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError.PersistenceError.NotFound(
                                entityType = "Scope",
                                entityId = parentId.value,
                            ),
                        )
                    }

                    // Calculate current hierarchy depth
                    val currentDepth = hierarchyApplicationService.calculateHierarchyDepth(parentId)
                        .mapLeft { error ->
                            applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                        }.bind()

                    // Validate depth limit
                    hierarchyService.validateHierarchyDepth(
                        newScopeId,
                        currentDepth,
                        hierarchyPolicy.maxDepth,
                    ).mapLeft { error ->
                        applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                    }.bind()

                    // Get existing children count
                    val existingChildren = scopeRepository.findByParentId(parentId, offset = 0, limit = 1000)
                        .mapLeft { error ->
                            applicationErrorMapper.mapDomainError(
                                error,
                                ErrorMappingContext(),
                            )
                        }.bind()

                    // Validate children limit
                    hierarchyService.validateChildrenLimit(
                        parentId,
                        existingChildren.size,
                        hierarchyPolicy.maxChildrenPerScope,
                    ).mapLeft { error ->
                        applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                    }.bind()
                }

                // Check title uniqueness at the same level
                val existingScopeId = scopeRepository.findIdByParentIdAndTitle(
                    parentId,
                    validatedTitle.value,
                ).mapLeft { error ->
                    applicationErrorMapper.mapDomainError(
                        error,
                        ErrorMappingContext(),
                    )
                }.bind()

                ensure(existingScopeId == null) {
                    applicationErrorMapper.mapToContractError(
                        io.github.kamiazya.scopes.scopemanagement.application.error.ScopeUniquenessError.DuplicateTitle(
                            title = command.title,
                            parentScopeId = parentId?.value,
                            existingScopeId = existingScopeId!!.value,
                        ),
                    )
                }

                // Always create scope with alias to satisfy contract requirement
                // Contract expects CreateScopeResult.canonicalAlias to be non-null
                val (finalAggregateResult, canonicalAlias) = if (command.customAlias != null) {
                    // Custom alias provided - validate format and create scope with custom alias
                    val aliasName = AliasName.create(command.customAlias).mapLeft { aliasError ->
                        logger.warn("Invalid custom alias format", mapOf("alias" to command.customAlias))
                        applicationErrorMapper.mapDomainError(
                            aliasError,
                            ErrorMappingContext(attemptedValue = command.customAlias),
                        )
                    }.bind()

                    // Create scope with custom alias in a single atomic operation
                    val resultWithAlias = ScopeAggregate.handleCreateWithAlias(
                        title = command.title,
                        description = command.description,
                        parentId = parentId,
                        aliasName = aliasName,
                        scopeId = newScopeId,
                        now = Clock.System.now(),
                    ).mapLeft { error ->
                        applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                    }.bind()

                    resultWithAlias to aliasName.value
                } else {
                    // Always generate alias automatically to satisfy contract requirement
                    // Even if generateAlias=false, we still create an alias because contract expects it
                    val resultWithAutoAlias = ScopeAggregate.handleCreateWithAutoAlias(
                        title = command.title,
                        description = command.description,
                        parentId = parentId,
                        scopeId = newScopeId,
                        now = Clock.System.now(),
                    ).mapLeft { error ->
                        applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                    }.bind()

                    // Extract the generated alias from the aggregate
                    val generatedAlias = resultWithAutoAlias.aggregate.canonicalAliasId?.let { id ->
                        resultWithAutoAlias.aggregate.aliases[id]?.aliasName?.value
                    }

                    resultWithAutoAlias to generatedAlias
                }

                // Persist events to EventStore AND project to RDB in same transaction
                // This implements the architectural pattern: ES decision + RDB projection
                eventSourcingRepository.persistScopeAggregate(finalAggregateResult).mapLeft { error ->
                    logger.error(
                        "Failed to persist events to EventStore",
                        mapOf("error" to error.toString()),
                    )
                    applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                }.bind()

                // Project all events to RDB in the same transaction
                // Extract events from envelopes since EventProjector expects List<DomainEvent>
                val domainEvents = finalAggregateResult.events.map { envelope -> envelope.event }
                eventProjector.projectEvents(domainEvents).mapLeft { error ->
                    logger.error(
                        "Failed to project events to RDB",
                        mapOf(
                            "error" to error.toString(),
                            "eventCount" to domainEvents.size.toString(),
                        ),
                    )
                    applicationErrorMapper.mapToContractError(error)
                }.bind()

                logger.info(
                    "Scope created successfully using EventSourcing",
                    mapOf(
                        "scopeId" to finalAggregateResult.aggregate.scopeId!!.value,
                        "hasAlias" to (canonicalAlias != null).toString(),
                    ),
                )

                // Extract scope data from aggregate for result mapping
                val aggregate = finalAggregateResult.aggregate
                val scope = io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope(
                    id = aggregate.scopeId!!,
                    title = aggregate.title!!,
                    description = aggregate.description,
                    parentId = aggregate.parentId,
                    status = aggregate.status,
                    aspects = aggregate.aspects,
                    createdAt = aggregate.createdAt,
                    updatedAt = aggregate.updatedAt,
                )

                val result = ScopeMapper.toCreateScopeResult(scope, canonicalAlias)

                logger.info(
                    "Scope creation workflow completed",
                    mapOf(
                        "scopeId" to scope.id.value,
                        "title" to scope.title.value,
                        "canonicalAlias" to (canonicalAlias ?: "none"),
                        "eventsCount" to domainEvents.size.toString(),
                    ),
                )

                result
            }
        }.bind()
    }.onLeft { error ->
        logger.error(
            "Failed to create scope using EventSourcing",
            mapOf(
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                "message" to error.toString(),
            ),
        )
    }
}
