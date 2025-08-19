package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.application.dto.CreateScopeResult
import io.github.kamiazya.scopes.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.application.logging.Logger
import io.github.kamiazya.scopes.application.port.TransactionManager
import io.github.kamiazya.scopes.application.service.CrossAggregateValidationService
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.command.CreateScope
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.domain.error.ScopeUniquenessError
import io.github.kamiazya.scopes.domain.error.ScopesError
import io.github.kamiazya.scopes.domain.error.currentTimestamp
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.domain.service.ScopeHierarchyService
import io.github.kamiazya.scopes.domain.valueobject.AliasName
import io.github.kamiazya.scopes.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Handler for CreateScope command with proper transaction management.
 *
 * Uses CoroutineContext for implicit logger context propagation.
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
    private val aliasManagementService: ScopeAliasManagementService,
    private val logger: Logger
) : UseCase<CreateScope, ScopesError, CreateScopeResult> {

    override suspend operator fun invoke(input: CreateScope): Either<ScopesError, CreateScopeResult> = either {
        val contextMetadata = mapOf(
            "title" to input.title,
            "parentId" to (input.parentId ?: "none"),
            "generateAlias" to input.generateAlias
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
                            currentTimestamp(),
                            parentIdString
                        )
                    }.bind()
                }

                // Validate hierarchy if parent is specified
                if (parentId != null) {
                    logger.debug("Validating hierarchy", mapOf("parentId" to parentId.value))

                    // Validate parent exists
                    crossAggregateValidationService.validateHierarchyConsistency(
                        parentId,
                        emptyList()
                    ).mapLeft { validationError ->
                        logger.error("Parent scope not found", mapOf("parentId" to parentId.value))
                        ScopeHierarchyError.ParentNotFound(
                            currentTimestamp(),
                            parentId,
                            parentId
                        )
                    }.bind()

                    // Calculate and validate hierarchy depth
                    val currentDepth = hierarchyService.calculateHierarchyDepth(
                        parentId,
                        { id -> scopeRepository.findById(id).getOrNull() }
                    ).bind()

                    logger.debug("Current hierarchy depth", mapOf(
                        "parentId" to parentId.value,
                        "depth" to currentDepth
                    ))

                    hierarchyService.validateHierarchyDepth(
                        parentId,
                        currentDepth
                    ).bind()

                    // Validate children limit
                    val existingChildren = scopeRepository.findByParentId(parentId).bind()
                    logger.debug("Checking children limit", mapOf(
                        "parentId" to parentId.value,
                        "existingChildren" to existingChildren.size
                    ))

                    hierarchyService.validateChildrenLimit(
                        parentId,
                        existingChildren.size
                    ).bind()
                }

                // Check title uniqueness at the same level
                logger.debug("Checking title uniqueness", mapOf(
                    "title" to input.title,
                    "parentId" to (parentId?.value ?: "null")
                ))

                val titleExists = scopeRepository.existsByParentIdAndTitle(
                    parentId,
                    input.title
                ).bind()

                ensure(!titleExists) {
                    logger.warn("Duplicate title found", mapOf(
                        "title" to input.title,
                        "parentId" to (parentId?.value ?: "null")
                    ))
                    ScopeUniquenessError.DuplicateTitle(
                        currentTimestamp(),
                        input.title,
                        parentId,
                        ScopeId.generate() // Placeholder for existing scope ID
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
                logger.debug("Creating scope entity", mapOf(
                    "title" to input.title,
                    "hasDescription" to (input.description != null),
                    "aspectCount" to aspects.size
                ))

                val scope = Scope.create(
                    title = input.title,
                    description = input.description,
                    parentId = parentId,
                    aspectsData = aspects
                ).bind()

                // Save the scope
                val savedScope = scopeRepository.save(scope).bind()
                logger.info("Scope saved successfully", mapOf("scopeId" to savedScope.id.value))

                // Handle alias generation or assignment
                val alias = when {
                    // If custom alias is provided, use it
                    input.customAlias != null -> {
                        logger.debug("Assigning custom alias", mapOf(
                            "scopeId" to savedScope.id.value,
                            "alias" to input.customAlias
                        ))
                        val aliasName = AliasName.create(input.customAlias).bind()
                        aliasManagementService.assignCanonicalAlias(savedScope.id, aliasName)
                            .onLeft { error ->
                                logger.warn("Failed to assign custom alias", mapOf(
                                    "scopeId" to savedScope.id.value,
                                    "alias" to input.customAlias,
                                    "error" to (error::class.simpleName ?: "Unknown")
                                ))
                            }
                            .getOrNull()
                    }
                    // If auto-generation is enabled, generate a canonical alias
                    input.generateAlias -> {
                        logger.debug("Generating canonical alias", mapOf("scopeId" to savedScope.id.value))
                        aliasManagementService.generateCanonicalAlias(savedScope.id)
                            .onLeft { error ->
                                logger.warn("Failed to generate alias", mapOf(
                                    "scopeId" to savedScope.id.value,
                                    "error" to (error::class.simpleName ?: "Unknown")
                                ))
                            }
                            .getOrNull()
                    }
                    else -> null
                }

                val result = ScopeMapper.toCreateScopeResult(savedScope, alias)

                logger.info("Scope created successfully", mapOf(
                    "scopeId" to savedScope.id.value,
                    "title" to savedScope.title,
                    "hasAlias" to (alias != null),
                    "aliasName" to (alias?.aliasName?.value ?: "none")
                ))

                result
            }
        }.bind()
    }.onLeft { error ->
        logger.error("Failed to create scope", mapOf(
            "error" to (error::class.simpleName ?: "Unknown"),
            "message" to error.toString()
        ))
    }
}

