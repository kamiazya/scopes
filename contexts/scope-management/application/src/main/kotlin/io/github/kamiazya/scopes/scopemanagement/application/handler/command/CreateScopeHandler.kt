package io.github.kamiazya.scopes.scopemanagement.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.scope.CreateScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.CreateScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.factory.ScopeFactory
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.port.HierarchyPolicyProvider
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
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
 */
class CreateScopeHandler(
    private val scopeFactory: ScopeFactory,
    private val scopeRepository: ScopeRepository,
    private val scopeAliasRepository: ScopeAliasRepository,
    private val aliasGenerationService: AliasGenerationService,
    private val transactionManager: TransactionManager,
    private val hierarchyPolicyProvider: HierarchyPolicyProvider,
    private val logger: Logger,
) : CommandHandler<CreateScopeCommand, ScopesError, CreateScopeResult> {

    override suspend operator fun invoke(command: CreateScopeCommand): Either<ScopesError, CreateScopeResult> = either {
        logger.info(
            "Creating new scope",
            mapOf(
                "title" to command.title,
                "parentId" to (command.parentId ?: "none"),
                "generateAlias" to command.generateAlias.toString(),
            ),
        )

        // Get hierarchy policy from external context
        val hierarchyPolicy = hierarchyPolicyProvider.getPolicy().bind()
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
                        ScopeHierarchyError.InvalidParentId(
                            occurredAt = Clock.System.now(),
                            invalidId = parentIdString,
                        )
                    }.bind()
                }

                // Delegate scope creation to factory
                val scopeAggregate = scopeFactory.createScope(
                    title = command.title,
                    description = command.description,
                    parentId = parentId,
                    hierarchyPolicy = hierarchyPolicy,
                ).bind()

                // Extract the scope from aggregate
                val scope = scopeAggregate.scope!!

                // Save the scope
                val savedScope = scopeRepository.save(scope).bind()
                logger.info("Scope saved successfully", mapOf("scopeId" to savedScope.id.value))

                // Handle alias generation and storage
                val canonicalAlias = if (command.generateAlias || command.customAlias != null) {
                    logger.debug(
                        "Processing alias generation",
                        mapOf(
                            "scopeId" to savedScope.id.value,
                            "generateAlias" to command.generateAlias.toString(),
                            "customAlias" to (command.customAlias ?: "none"),
                        ),
                    )

                    // Determine alias name based on command
                    val aliasName = if (command.customAlias != null) {
                        // Custom alias provided - validate format
                        logger.debug("Validating custom alias", mapOf("customAlias" to command.customAlias))
                        AliasName.create(command.customAlias).mapLeft { aliasError ->
                            logger.warn("Invalid custom alias format", mapOf("alias" to command.customAlias, "error" to aliasError.toString()))
                            aliasError
                        }.bind()
                    } else {
                        // Generate alias automatically
                        logger.debug("Generating automatic alias")
                        aliasGenerationService.generateRandomAlias().mapLeft { aliasError ->
                            logger.error("Failed to generate alias", mapOf("scopeId" to savedScope.id.value, "error" to aliasError.toString()))
                            aliasError
                        }.bind()
                    }

                    // Check if alias already exists
                    // Check if alias already exists and get the existing scope ID if it does
                    val existingAlias = scopeAliasRepository.findByAliasName(aliasName).bind()
                    ensure(existingAlias == null) {
                        val duplicateError = ScopeAliasError.DuplicateAlias(
                            occurredAt = Clock.System.now(),
                            aliasName = aliasName.value,
                            existingScopeId = existingAlias!!.scopeId, // The actual scope that owns this alias
                            attemptedScopeId = savedScope.id, // The new scope that tried to use it
                        )
                        logger.warn(
                            "Alias already exists",
                            mapOf(
                                "alias" to aliasName.value,
                                "existingScopeId" to existingAlias.scopeId.value,
                                "attemptedScopeId" to savedScope.id.value,
                            ),
                        )
                        duplicateError
                    }

                    // Create and save the canonical alias
                    val scopeAlias = ScopeAlias.createCanonical(savedScope.id, aliasName, Clock.System.now())
                    scopeAliasRepository.save(scopeAlias).bind()

                    logger.info("Canonical alias created successfully", mapOf("alias" to aliasName.value, "scopeId" to savedScope.id.value))
                    aliasName.value
                } else {
                    null
                }

                val result = ScopeMapper.toCreateScopeResult(savedScope, canonicalAlias)

                logger.info(
                    "Scope created successfully",
                    mapOf(
                        "scopeId" to savedScope.id.value,
                        "title" to savedScope.title.value,
                        "hasAlias" to (canonicalAlias != null).toString(),
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
