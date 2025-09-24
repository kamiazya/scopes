package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.domain.aggregate.AggregateResult
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.port.EventPublisher
import io.github.kamiazya.scopes.scopemanagement.application.port.HierarchyPolicyProvider
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeHierarchyApplicationService
import io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAggregate
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeEvent
import io.github.kamiazya.scopes.scopemanagement.domain.extensions.persistScopeAggregate
import io.github.kamiazya.scopes.scopemanagement.domain.repository.EventSourcingRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.hierarchy.ScopeHierarchyService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.HierarchyPolicy
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
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
    private val eventPublisher: EventPublisher,
    private val applicationErrorMapper: ApplicationErrorMapper,
    private val logger: Logger,
) : CommandHandler<CreateScopeCommand, ScopeContractError, CreateScopeResult> {

    override suspend operator fun invoke(command: CreateScopeCommand): Either<ScopeContractError, CreateScopeResult> = either {
        logCommandStart(command)

        val hierarchyPolicy = getHierarchyPolicy().bind()

        transactionManager.inTransaction {
            either {
                val validationResult = validateCommand(command).bind()
                val aggregateResult = createScopeAggregate(command, validationResult).bind()
                persistScopeAggregate(aggregateResult).bind()
                buildResult(aggregateResult, validationResult.canonicalAlias).bind()
            }
        }.bind()
    }.onLeft { error -> logCommandFailure(error) }

    private fun logCommandStart(command: CreateScopeCommand) {
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
    }

    private suspend fun getHierarchyPolicy(): Either<ScopeContractError, HierarchyPolicy> = either {
        hierarchyPolicyProvider.getPolicy()
            .mapLeft { error -> applicationErrorMapper.mapDomainError(error, ErrorMappingContext()) }
            .bind()
    }

    private data class ValidatedInput(val parentId: ScopeId?, val validatedTitle: ScopeTitle, val newScopeId: ScopeId, val canonicalAlias: String?)

    private suspend fun validateCommand(command: CreateScopeCommand): Either<ScopeContractError, ValidatedInput> = either {
        val parentId = parseParentId(command.parentId).bind()
        val validatedTitle = validateTitle(command.title).bind()
        val newScopeId = ScopeId.generate()

        if (parentId != null) {
            validateHierarchyConstraints(parentId, newScopeId).bind()
        }

        validateTitleUniqueness(parentId, validatedTitle).bind()

        val canonicalAlias = when (command) {
            is CreateScopeCommand.WithCustomAlias -> command.alias
            is CreateScopeCommand.WithAutoAlias -> null
        }

        ValidatedInput(parentId, validatedTitle, newScopeId, canonicalAlias)
    }

    private suspend fun parseParentId(parentIdString: String?): Either<ScopeContractError, ScopeId?> = either {
        parentIdString?.let { idString ->
            ScopeId.create(idString).mapLeft { idError ->
                logger.warn("Invalid parent ID format", mapOf("parentId" to idString))
                applicationErrorMapper.mapDomainError(
                    idError,
                    ErrorMappingContext(attemptedValue = idString),
                )
            }.bind()
        }
    }

    private suspend fun validateTitle(title: String): Either<ScopeContractError, ScopeTitle> = either {
        ScopeTitle.create(title)
            .mapLeft { titleError ->
                applicationErrorMapper.mapDomainError(
                    titleError,
                    ErrorMappingContext(attemptedValue = title),
                )
            }.bind()
    }

    private suspend fun validateHierarchyConstraints(parentId: ScopeId, newScopeId: ScopeId): Either<ScopeContractError, Unit> = either {
        val hierarchyPolicy = getHierarchyPolicy().bind()

        validateParentExists(parentId).bind()
        validateDepthLimit(parentId, newScopeId, hierarchyPolicy).bind()
        validateChildrenLimit(parentId, hierarchyPolicy).bind()
    }

    private suspend fun validateParentExists(parentId: ScopeId): Either<ScopeContractError, Unit> = either {
        val parentExists = scopeRepository.existsById(parentId)
            .mapLeft { error ->
                applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
            }.bind()

        ensure(parentExists) {
            applicationErrorMapper.mapToContractError(
                io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError.PersistenceError.NotFound(
                    entityType = "Scope",
                    entityId = parentId.value,
                ),
            )
        }
    }

    private suspend fun validateDepthLimit(parentId: ScopeId, newScopeId: ScopeId, hierarchyPolicy: HierarchyPolicy): Either<ScopeContractError, Unit> =
        either {
            val currentDepth = hierarchyApplicationService.calculateHierarchyDepth(parentId)
                .mapLeft { error ->
                    applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                }.bind()

            hierarchyService.validateHierarchyDepth(
                newScopeId,
                currentDepth,
                hierarchyPolicy.maxDepth,
            ).mapLeft { error ->
                applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
            }.bind()
        }

    private suspend fun validateChildrenLimit(parentId: ScopeId, hierarchyPolicy: HierarchyPolicy): Either<ScopeContractError, Unit> = either {
        val existingChildren = scopeRepository.findByParentId(parentId, offset = 0, limit = 1000)
            .mapLeft { error ->
                applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
            }.bind()

        hierarchyService.validateChildrenLimit(
            parentId,
            existingChildren.size,
            hierarchyPolicy.maxChildrenPerScope,
        ).mapLeft { error ->
            applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
        }.bind()
    }

    private suspend fun validateTitleUniqueness(parentId: ScopeId?, validatedTitle: ScopeTitle): Either<ScopeContractError, Unit> = either {
        val existingScopeId = scopeRepository.findIdByParentIdAndTitle(
            parentId,
            validatedTitle.value,
        ).mapLeft { error ->
            applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
        }.bind()

        ensure(existingScopeId == null) {
            applicationErrorMapper.mapToContractError(
                io.github.kamiazya.scopes.scopemanagement.application.error.ScopeUniquenessError.DuplicateTitle(
                    title = validatedTitle.value,
                    parentScopeId = parentId?.value,
                    existingScopeId = existingScopeId!!.value,
                ),
            )
        }
    }

    private suspend fun createScopeAggregate(
        command: CreateScopeCommand,
        validationResult: ValidatedInput,
    ): Either<ScopeContractError, AggregateResult<ScopeAggregate, ScopeEvent>> = either {
        when (command) {
            is CreateScopeCommand.WithCustomAlias -> {
                val aliasName = AliasName.create(command.alias).mapLeft { aliasError ->
                    logger.warn("Invalid custom alias format", mapOf("alias" to command.alias))
                    applicationErrorMapper.mapDomainError(
                        aliasError,
                        ErrorMappingContext(attemptedValue = command.alias),
                    )
                }.bind()

                ScopeAggregate.handleCreateWithAlias(
                    title = command.title,
                    description = command.description,
                    parentId = validationResult.parentId,
                    aliasName = aliasName,
                    scopeId = validationResult.newScopeId,
                    now = Clock.System.now(),
                ).mapLeft { error ->
                    applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                }.bind()
            }
            is CreateScopeCommand.WithAutoAlias -> {
                ScopeAggregate.handleCreateWithAutoAlias(
                    title = command.title,
                    description = command.description,
                    parentId = validationResult.parentId,
                    scopeId = validationResult.newScopeId,
                    now = Clock.System.now(),
                ).mapLeft { error ->
                    applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
                }.bind()
            }
        }
    }

    private suspend fun persistScopeAggregate(aggregateResult: AggregateResult<ScopeAggregate, ScopeEvent>): Either<ScopeContractError, Unit> = either {
        eventSourcingRepository.persistScopeAggregate(aggregateResult).mapLeft { error ->
            logger.error(
                "Failed to persist events to EventStore",
                mapOf("error" to error.toString()),
            )
            applicationErrorMapper.mapDomainError(error, ErrorMappingContext())
        }.bind()

        val domainEvents = aggregateResult.events.map { envelope -> envelope.event }
        eventPublisher.projectEvents(domainEvents).mapLeft { error ->
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
                "scopeId" to aggregateResult.aggregate.scopeId!!.value,
                "eventsCount" to domainEvents.size.toString(),
            ),
        )
    }

    private suspend fun buildResult(
        aggregateResult: AggregateResult<ScopeAggregate, ScopeEvent>,
        commandCanonicalAlias: String?,
    ): Either<ScopeContractError, CreateScopeResult> = either {
        val aggregate = aggregateResult.aggregate
        val resolvedAlias = commandCanonicalAlias ?: run {
            aggregate.canonicalAliasId?.let { id ->
                aggregate.aliases[id]?.aliasName?.value
            }
        }

        ensure(resolvedAlias != null) {
            // Create の仕様上必ず Canonical Alias が存在するはず。存在しないのは投影/適用不整合。
            ScopeContractError.DataInconsistency.MissingCanonicalAlias(
                scopeId = aggregate.scopeId?.value ?: "",
            )
        }

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

        val result = ScopeMapper.toCreateScopeResult(scope, resolvedAlias!!)

        logger.info(
            "Scope creation workflow completed",
            mapOf(
                "scopeId" to scope.id.value,
                "title" to scope.title.value,
                "canonicalAlias" to resolvedAlias,
            ),
        )

        result
    }

    private fun logCommandFailure(error: ScopeContractError) {
        logger.error(
            "Failed to create scope using EventSourcing",
            mapOf(
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: error::class.toString()),
                "message" to error.toString(),
            ),
        )
    }
}
