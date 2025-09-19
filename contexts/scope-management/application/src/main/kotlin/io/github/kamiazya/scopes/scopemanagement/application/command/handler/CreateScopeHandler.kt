package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.CreateScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.CreateScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.factory.ScopeFactory
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.port.HierarchyPolicyProvider
import io.github.kamiazya.scopes.scopemanagement.application.service.error.CentralizedErrorMappingService
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.alias.AliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

/**
 * Handler for CreateScope command.
 * Uses BaseCommandHandler for common functionality and centralized error mapping.
 */
class CreateScopeHandler(
    private val scopeFactory: ScopeFactory,
    private val scopeRepository: ScopeRepository,
    private val scopeAliasRepository: ScopeAliasRepository,
    private val aliasGenerationService: AliasGenerationService,
    private val hierarchyPolicyProvider: HierarchyPolicyProvider,
    transactionManager: TransactionManager,
    logger: Logger,
) : BaseCommandHandler<CreateScopeCommand, CreateScopeResult>(transactionManager, logger) {

    private val errorMappingService = CentralizedErrorMappingService()

    override suspend fun executeCommand(command: CreateScopeCommand): Either<ScopeManagementApplicationError, CreateScopeResult> = either {
        // Get hierarchy policy from external context
        val hierarchyPolicy = hierarchyPolicyProvider.getPolicy().bind()
        logger.debug(
            "Hierarchy policy loaded",
            mapOf(
                "maxDepth" to hierarchyPolicy.maxDepth.toString(),
                "maxChildrenPerScope" to hierarchyPolicy.maxChildrenPerScope.toString(),
            ),
        )
        // Parse parent ID if provided
        val parentId = command.parentId?.let { parentIdString ->
            ScopeId.create(parentIdString).mapLeft { idError ->
                logger.warn("Invalid parent ID format", mapOf("parentId" to parentIdString))
                errorMappingService.mapScopeIdError(idError, parentIdString, "create-scope-parent")
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
        val savedScope = scopeRepository.save(scope).mapLeft {
            errorMappingService.mapRepositoryError(it, "create-scope-save")
        }.bind()
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
                    errorMappingService.mapDomainError(aliasError, "create-scope-custom-alias")
                }.bind()
            } else {
                // Generate alias automatically
                logger.debug("Generating automatic alias")
                aliasGenerationService.generateRandomAlias().mapLeft { aliasError ->
                    logger.error("Failed to generate alias", mapOf("scopeId" to savedScope.id.value, "error" to aliasError.toString()))
                    errorMappingService.mapDomainError(aliasError, "create-scope-generate-alias")
                }.bind()
            }

            // Check if alias already exists
            val existingAlias = scopeAliasRepository.findByAliasName(aliasName).mapLeft {
                errorMappingService.mapRepositoryError(it, "create-scope-check-alias")
            }.bind()
            ensure(existingAlias == null) {
                val duplicateError = ScopeAliasError.AliasDuplicate(
                    aliasName = aliasName.value,
                    existingScopeId = existingAlias!!.scopeId.value,
                    attemptedScopeId = savedScope.id.value,
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
            scopeAliasRepository.save(scopeAlias).mapLeft {
                errorMappingService.mapRepositoryError(it, "create-scope-save-alias")
            }.bind()

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
}
