package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.CreateScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.CreateScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.port.HierarchyPolicyProvider
import io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAggregate
import io.github.kamiazya.scopes.scopemanagement.domain.extensions.persistScopeAggregate
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.scopemanagement.domain.repository.EventSourcingRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.alias.AliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

/**
 * V2 Handler for CreateScope command using Event Sourcing pattern.
 *
 * This handler demonstrates the new event-sourced approach where:
 * - Scope and alias management is unified in ScopeAggregate
 * - All changes go through domain events
 * - EventSourcingRepository handles persistence
 * - No separate ScopeAliasRepository needed
 *
 * This will replace the old CreateScopeHandler after migration is complete.
 */
class CreateScopeHandlerV2(
    private val eventSourcingRepository: EventSourcingRepository<DomainEvent>,
    private val aliasGenerationService: AliasGenerationService,
    private val transactionManager: TransactionManager,
    private val hierarchyPolicyProvider: HierarchyPolicyProvider,
    private val applicationErrorMapper: ApplicationErrorMapper,
    private val logger: Logger,
) : CommandHandler<CreateScopeCommand, ScopeContractError, CreateScopeResult> {

    override suspend operator fun invoke(command: CreateScopeCommand): Either<ScopeContractError, CreateScopeResult> = either {
        logger.info(
            "Creating new scope using EventSourcing pattern",
            mapOf(
                "title" to command.title,
                "parentId" to (command.parentId ?: "none"),
                "generateAlias" to command.generateAlias.toString(),
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

                val (finalAggregateResult, canonicalAlias) = if (command.generateAlias || command.customAlias != null) {
                    // Determine alias name
                    val aliasName = if (command.customAlias != null) {
                        // Custom alias provided - validate format
                        AliasName.create(command.customAlias).mapLeft { aliasError ->
                            logger.warn("Invalid custom alias format", mapOf("alias" to command.customAlias))
                            applicationErrorMapper.mapDomainError(
                                aliasError,
                                ErrorMappingContext(attemptedValue = command.customAlias),
                            )
                        }.bind()
                    } else {
                        // Generate alias automatically
                        aliasGenerationService.generateRandomAlias().mapLeft { aliasError ->
                            logger.error("Failed to generate alias")
                            applicationErrorMapper.mapDomainError(
                                aliasError,
                                ErrorMappingContext(),
                            )
                        }.bind()
                    }

                    // Create scope with alias in a single atomic operation
                    val resultWithAlias = ScopeAggregate.handleCreateWithAlias(
                        title = command.title,
                        description = command.description,
                        parentId = parentId,
                        aliasName = aliasName,
                        now = Clock.System.now(),
                    ).mapLeft { error ->
                        applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                    }.bind()

                    resultWithAlias to aliasName.value
                } else {
                    // Create scope without alias
                    val simpleResult = ScopeAggregate.handleCreate(
                        title = command.title,
                        description = command.description,
                        parentId = parentId,
                        now = Clock.System.now(),
                    ).mapLeft { error ->
                        applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                    }.bind()
                    
                    simpleResult to null
                }

                // Persist all events (scope creation + alias assignment if applicable)
                eventSourcingRepository.persistScopeAggregate(finalAggregateResult).mapLeft { error ->
                    applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                }.bind()

                logger.info(
                    "Scope created successfully using EventSourcing", 
                    mapOf(
                        "scopeId" to finalAggregateResult.aggregate.scopeId!!.value,
                        "hasAlias" to (canonicalAlias != null).toString(),
                    )
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
                        "eventsCount" to finalAggregateResult.events.size.toString(),
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