package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.factory.ScopeFactory
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.port.HierarchyPolicyProvider
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.alias.AliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

/**
 * Handler for CreateScope command with proper transaction management.
 *
 * Following Clean Architecture and DDD principles:
 * - Uses TransactionManager for atomic operations
 * - Delegates scope creation to ScopeFactory
 * - Retrieves hierarchy policy from external context via port
 * - Maintains clear separation of concerns with minimal orchestration logic
 *
 * Note: This handler returns contract errors directly as part of a pilot
 * to simplify error handling architecture. It uses ApplicationErrorMapper
 * for factory errors as a pragmatic compromise during the transition.
 */
class CreateScopeHandler(
    private val scopeFactory: ScopeFactory,
    private val scopeRepository: ScopeRepository,
    private val scopeAliasRepository: ScopeAliasRepository,
    private val aliasGenerationService: AliasGenerationService,
    private val transactionManager: TransactionManager,
    private val hierarchyPolicyProvider: HierarchyPolicyProvider,
    private val applicationErrorMapper: ApplicationErrorMapper,
    private val logger: Logger,
) : CommandHandler<CreateScopeCommand, ScopeContractError, CreateScopeResult> {

    override suspend operator fun invoke(command: CreateScopeCommand): Either<ScopeContractError, CreateScopeResult> = either {
        val aliasStrategy = when (command) {
            is CreateScopeCommand.WithAutoAlias -> "auto"
            is CreateScopeCommand.WithCustomAlias -> "custom"
        }

        logger.info(
            "Creating new scope",
            mapOf(
                "title" to command.title,
                "parentId" to (command.parentId ?: "none"),
                "aliasStrategy" to aliasStrategy,
            ),
        )

        // Get hierarchy policy from external context
        val hierarchyPolicy = hierarchyPolicyProvider.getPolicy()
            .mapLeft { error -> applicationErrorMapper.mapDomainError(error) }
            .bind()
        logger.debug(
            "Hierarchy policy loaded",
            mapOf(
                "maxDepth" to hierarchyPolicy.maxDepth.toString(),
                "maxChildrenPerScope" to hierarchyPolicy.maxChildrenPerScope.toString(),
            ),
        )

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

                // Delegate scope creation to factory
                val scopeAggregate = scopeFactory.createScope(
                    title = command.title,
                    description = command.description,
                    parentId = parentId,
                    hierarchyPolicy = hierarchyPolicy,
                ).mapLeft { error ->
                    // Map application error to contract error
                    applicationErrorMapper.mapToContractError(error)
                }.bind()

                // Extract the scope from aggregate
                val scope = scopeAggregate.scope!!

                // Save the scope
                val savedScope = scopeRepository.save(scope).mapLeft { error ->
                    applicationErrorMapper.mapDomainError(error)
                }.bind()
                logger.info("Scope saved successfully", mapOf("scopeId" to savedScope.id.value))

                // Handle alias generation and storage (always generate an alias for consistency with contract)
                val canonicalAlias = run {
                    logger.debug(
                        "Processing alias generation",
                        mapOf(
                            "scopeId" to savedScope.id.value,
                            "aliasStrategy" to aliasStrategy,
                        ),
                    )

                    // Determine alias name based on command variant
                    val aliasName = when (command) {
                        is CreateScopeCommand.WithCustomAlias -> {
                            // Custom alias provided - validate format
                            logger.debug("Validating custom alias", mapOf("customAlias" to command.alias))
                            AliasName.create(command.alias).mapLeft { aliasError ->
                                logger.warn("Invalid custom alias format", mapOf("alias" to command.alias, "error" to aliasError.toString()))
                                applicationErrorMapper.mapDomainError(
                                    aliasError,
                                    ErrorMappingContext(attemptedValue = command.alias),
                                )
                            }.bind()
                        }
                        is CreateScopeCommand.WithAutoAlias -> {
                            // Generate alias automatically
                            logger.debug("Generating automatic alias")
                            aliasGenerationService.generateRandomAlias().mapLeft { aliasError ->
                                logger.error("Failed to generate alias", mapOf("scopeId" to savedScope.id.value, "error" to aliasError.toString()))
                                applicationErrorMapper.mapDomainError(
                                    aliasError,
                                    ErrorMappingContext(scopeId = savedScope.id.value),
                                )
                            }.bind()
                        }
                    }

                    // Create and save the canonical alias (Insert-First strategy)
                    // Let the database unique constraint handle duplicates
                    val scopeAlias = ScopeAlias.createCanonical(savedScope.id, aliasName, Clock.System.now())
                    scopeAliasRepository.save(scopeAlias).mapLeft { error ->
                        applicationErrorMapper.mapDomainError(error)
                    }.bind()

                    logger.info("Canonical alias created successfully", mapOf("alias" to aliasName.value, "scopeId" to savedScope.id.value))
                    aliasName.value
                }

                val result = ScopeMapper.toCreateScopeResult(savedScope, canonicalAlias)

                logger.info(
                    "Scope created successfully",
                    mapOf(
                        "scopeId" to savedScope.id.value,
                        "title" to savedScope.title.value,
                        "aliasStrategy" to aliasStrategy,
                        "aliasValue" to canonicalAlias,
                    ),
                )

                result
            }
        }.bind()
    }.onLeft { error ->
        logger.error(
            "Failed to create scope",
            mapOf(
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                "message" to error.toString(),
            ),
        )
    }
}
