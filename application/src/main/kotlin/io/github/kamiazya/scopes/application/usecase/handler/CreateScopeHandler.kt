package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.nonEmptyListOf
import io.github.kamiazya.scopes.application.dto.CreateScopeResult
import io.github.kamiazya.scopes.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.application.port.TransactionManager
import io.github.kamiazya.scopes.application.service.CrossAggregateValidationService
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.command.CreateScope
import io.github.kamiazya.scopes.domain.error.ScopesError
import io.github.kamiazya.scopes.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.domain.error.ScopeUniquenessError
import io.github.kamiazya.scopes.domain.error.PersistenceError
import io.github.kamiazya.scopes.domain.error.currentTimestamp
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.service.ScopeHierarchyService
import io.github.kamiazya.scopes.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.valueobject.AliasName

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
    private val aliasManagementService: ScopeAliasManagementService
) : UseCase<CreateScope, ScopesError, CreateScopeResult> {

    override suspend operator fun invoke(input: CreateScope): Either<ScopesError, CreateScopeResult> = 
        transactionManager.inTransaction {
            either {
                // Parse parent ID if provided
                val parentId = input.parentId?.let { parentIdString ->
                    ScopeId.create(parentIdString).mapLeft { idError ->
                        // Map ID validation error to hierarchy error for backward compatibility
                        ScopeHierarchyError.InvalidParentId(
                            currentTimestamp(),
                            parentIdString
                        )
                    }.bind()
                }
                
                // Validate hierarchy if parent is specified
                if (parentId != null) {
                    // Validate parent exists using cross-aggregate validation
                    crossAggregateValidationService.validateHierarchyConsistency(
                        parentId,
                        emptyList() // No children yet
                    ).mapLeft { validationError ->
                        // Map cross-aggregate error to domain error
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
                    
                    hierarchyService.validateHierarchyDepth(
                        parentId,
                        currentDepth
                    ).bind()
                    
                    // Validate children limit
                    val existingChildren = scopeRepository.findByParentId(parentId).bind()
                    hierarchyService.validateChildrenLimit(
                        parentId,
                        existingChildren.size
                    ).bind()
                }
                
                // Check title uniqueness at the same level
                val titleExists = scopeRepository.existsByParentIdAndTitle(
                    parentId,
                    input.title
                ).bind()
                
                if (titleExists) {
                    // DuplicateTitle needs existingScopeId - we'll use a generated one for now
                    // In a real implementation, the repository would return the existing scope ID
                    raise(ScopeUniquenessError.DuplicateTitle(
                        currentTimestamp(),
                        input.title,
                        parentId,
                        ScopeId.generate() // Placeholder for existing scope ID
                    ))
                }
                
                // Convert metadata to aspects
                val aspects = input.metadata.mapNotNull { (key, value) ->
                    val aspectKey = AspectKey.create(key).getOrNull()
                    val aspectValue = AspectValue.create(value).getOrNull()
                    if (aspectKey != null && aspectValue != null) {
                        aspectKey to nonEmptyListOf(aspectValue)
                    } else {
                        null
                    }
                }.toMap()

                // Create domain entity
                val scope = Scope.create(
                    title = input.title,
                    description = input.description,
                    parentId = parentId,
                    aspectsData = aspects
                ).bind()

                // Save the scope
                val savedScope = scopeRepository.save(scope).bind()
                
                // Handle alias generation or assignment
                val alias = when {
                    // If custom alias is provided, use it
                    input.customAlias != null -> {
                        val aliasName = AliasName.create(input.customAlias).bind()
                        aliasManagementService.assignCanonicalAlias(savedScope.id, aliasName)
                            .getOrNull() // Continue even if alias assignment fails
                    }
                    // If auto-generation is enabled, generate a canonical alias
                    input.generateAlias -> {
                        aliasManagementService.generateCanonicalAlias(savedScope.id)
                            .getOrNull() // Continue even if alias generation fails
                    }
                    else -> null
                }

                ScopeMapper.toCreateScopeResult(savedScope, alias)
            }
        }
}