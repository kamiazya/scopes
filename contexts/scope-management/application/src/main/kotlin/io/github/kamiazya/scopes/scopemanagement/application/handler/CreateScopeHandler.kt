package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.CreateScope
import io.github.kamiazya.scopes.scopemanagement.application.dto.CreateScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.port.HierarchyPolicyProvider
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.factory.ScopeFactory
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
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
    private val transactionManager: TransactionManager,
    private val hierarchyPolicyProvider: HierarchyPolicyProvider,
    private val logger: Logger,
) : UseCase<CreateScope, ScopesError, CreateScopeResult> {

    override suspend operator fun invoke(input: CreateScope): Either<ScopesError, CreateScopeResult> = either {
        logger.info(
            "Creating new scope",
            mapOf(
                "title" to input.title,
                "parentId" to (input.parentId ?: "none"),
                "generateAlias" to input.generateAlias.toString(),
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
                val parentId = input.parentId?.let { parentIdString ->
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
                    title = input.title,
                    description = input.description,
                    parentId = parentId,
                    hierarchyPolicy = hierarchyPolicy,
                ).bind()

                // Extract the scope from aggregate
                val scope = scopeAggregate.scope!!

                // Save the scope
                val savedScope = scopeRepository.save(scope).bind()
                logger.info("Scope saved successfully", mapOf("scopeId" to savedScope.id.value))

                // Handle alias generation (future integration)
                val canonicalAlias = if (input.generateAlias || input.customAlias != null) {
                    logger.debug(
                        "Alias generation requested but not yet implemented",
                        mapOf(
                            "scopeId" to savedScope.id.value,
                            "generateAlias" to input.generateAlias.toString(),
                            "customAlias" to (input.customAlias ?: "none"),
                        ),
                    )
                    null
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
                "error" to (error::class.simpleName ?: "Unknown"),
                "message" to error.toString(),
            ),
        )
    }
}
