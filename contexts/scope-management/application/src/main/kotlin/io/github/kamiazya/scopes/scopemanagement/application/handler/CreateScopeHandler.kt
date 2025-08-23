package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.CreateScope
import io.github.kamiazya.scopes.scopemanagement.application.dto.CreateScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.service.CrossAggregateValidationService
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeUniquenessError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeHierarchyService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Aspects
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

/**
 * Handler for CreateScope command with proper transaction management.
 *
 * Following Clean Architecture and DDD principles:
 * - Uses TransactionManager for atomic operations
 * - Delegates hierarchy validation to domain service
 * - Uses CrossAggregateValidationService for cross-aggregate invariants
 * - Maintains clear separation of concerns
 */
class CreateScopeHandler(
    private val scopeRepository: ScopeRepository,
    private val transactionManager: TransactionManager,
    private val hierarchyService: ScopeHierarchyService,
    private val crossAggregateValidationService: CrossAggregateValidationService,
    private val logger: Logger,
) : UseCase<CreateScope, ScopesError, CreateScopeResult> {

    override suspend operator fun invoke(input: CreateScope): Either<ScopesError, CreateScopeResult> = either {
        val contextMetadata = mapOf(
            "title" to input.title,
            "parentId" to (input.parentId ?: "none"),
            "generateAlias" to input.generateAlias.toString(),
        )

        logger.info("Creating new scope", contextMetadata)

        transactionManager.inTransaction {
            either {
                // Parse parent ID if provided
                val parentId = input.parentId?.let { parentIdString ->
                    logger.debug("Parsing parent ID", mapOf("parentId" to parentIdString))
                    ScopeId.create(parentIdString).mapLeft { idError ->
                        logger.warn("Invalid parent ID format", mapOf("parentId" to parentIdString))
                        ScopeHierarchyError.InvalidParentId(
                            occurredAt = Clock.System.now(),
                            invalidId = parentIdString,
                        )
                    }.bind()
                }

                // Validate hierarchy if parent is specified
                if (parentId != null) {
                    logger.debug("Validating hierarchy", mapOf("parentId" to parentId.value))

                    // Validate parent exists
                    crossAggregateValidationService.validateHierarchyConsistency(
                        parentId,
                        emptyList(),
                    ).mapLeft { validationError ->
                        logger.error("Parent scope not found", mapOf("parentId" to parentId.value))
                        ScopeHierarchyError.ParentNotFound(
                            occurredAt = Clock.System.now(),
                            scopeId = parentId,
                            parentId = parentId,
                        )
                    }.bind()

                    // Calculate and validate hierarchy depth
                    val currentDepth = hierarchyService.calculateHierarchyDepth(
                        parentId,
                        { id -> scopeRepository.findById(id).getOrNull() },
                    ).bind()

                    logger.debug(
                        "Current hierarchy depth",
                        mapOf(
                            "parentId" to parentId.value,
                            "depth" to currentDepth.toString(),
                        ),
                    )

                    hierarchyService.validateHierarchyDepth(
                        parentId,
                        currentDepth,
                    ).bind()

                    // Validate children limit
                    val existingChildren = scopeRepository.findByParentId(parentId).bind()
                    logger.debug(
                        "Checking children limit",
                        mapOf(
                            "parentId" to parentId.value,
                            "existingChildren" to existingChildren.size.toString(),
                        ),
                    )

                    hierarchyService.validateChildrenLimit(
                        parentId,
                        existingChildren.size,
                    ).bind()
                }

                // Check title uniqueness at the same level
                logger.debug(
                    "Checking title uniqueness",
                    mapOf(
                        "title" to input.title,
                        "parentId" to (parentId?.value ?: "null"),
                    ),
                )

                val titleExists = scopeRepository.existsByParentIdAndTitle(
                    parentId,
                    input.title,
                ).bind()

                ensure(!titleExists) {
                    logger.warn(
                        "Duplicate title found",
                        mapOf(
                            "title" to input.title,
                            "parentId" to (parentId?.value ?: "null"),
                        ),
                    )
                    ScopeUniquenessError.DuplicateTitle(
                        occurredAt = Clock.System.now(),
                        title = input.title,
                        parentScopeId = parentId,
                        existingScopeId = ScopeId.generate(), // Placeholder for existing scope ID
                    )
                }

                // Convert metadata to aspects
                val aspects = input.metadata.mapNotNull { (key, value) ->
                    val aspectKey = AspectKey.create(key).getOrNull()
                    val aspectValue = AspectValue.create(value).getOrNull()
                    if (aspectKey != null && aspectValue != null) {
                        aspectKey to nonEmptyListOf(aspectValue)
                    } else {
                        logger.debug("Skipping invalid aspect", mapOf("key" to key, "value" to value))
                        null
                    }
                }.toMap()

                // Create domain entity
                logger.debug(
                    "Creating scope entity",
                    mapOf(
                        "title" to input.title,
                        "hasDescription" to (input.description != null).toString(),
                        "aspectCount" to aspects.size.toString(),
                    ),
                )

                val scope = Scope.create(
                    title = input.title,
                    description = input.description,
                    parentId = parentId,
                    aspects = Aspects.from(aspects),
                ).bind()

                // Save the scope
                val savedScope = scopeRepository.save(scope).bind()
                logger.info("Scope saved successfully", mapOf("scopeId" to savedScope.id.value))

                // For now, we don't handle alias generation as it's in a different bounded context
                // This would be handled through domain events or a separate service call
                val canonicalAlias = if (input.generateAlias || input.customAlias != null) {
                    // TODO: Integrate with Alias Management context
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
