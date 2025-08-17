package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.application.dto.ContextViewResult
import io.github.kamiazya.scopes.application.dto.FilteredScopesResult
import io.github.kamiazya.scopes.application.dto.ScopeResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.error.DomainErrorMapper
import io.github.kamiazya.scopes.application.service.ActiveContextService
import io.github.kamiazya.scopes.application.usecase.query.GetFilteredScopes
import io.github.kamiazya.scopes.domain.entity.ContextView
import io.github.kamiazya.scopes.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.valueobject.ContextName

/**
 * Handler for getting filtered scopes based on a context view.
 * 
 * This handler:
 * 1. Resolves the context view (by name or active context)
 * 2. Parses the filter expression
 * 3. Retrieves scopes matching the filter
 * 4. Returns the filtered results with metadata
 */
class GetFilteredScopesHandler(
    private val contextViewRepository: ContextViewRepository,
    private val scopeRepository: ScopeRepository,
    private val activeContextService: ActiveContextService
) : QueryHandler<GetFilteredScopes, FilteredScopesResult> {

    override suspend fun invoke(query: GetFilteredScopes): Either<ApplicationError, FilteredScopesResult> {
        // Resolve the context view
        val contextViewResult: Either<ApplicationError, ContextView> = if (query.contextName != null) {
            // Find by name
            ContextName.create(query.contextName).mapLeft { errorMessage ->
                ApplicationError.ContextError.NamingInvalidFormat(
                    attemptedName = query.contextName
                )
            }.flatMap { contextName ->
                contextViewRepository.findByName(contextName).mapLeft { error ->
                    DomainErrorMapper.mapToApplicationError(error)
                }.flatMap { contextView ->
                    contextView?.right() ?: ApplicationError.ContextError.StateNotFound(
                        contextName = query.contextName
                    ).left()
                }
            }
        } else {
            // Use active context
            val activeContext = activeContextService.getCurrentContext()
            activeContext?.right() ?: ApplicationError.ContextError.StateNotFound(
                contextName = "Active context"
            ).left()
        }
        
        return contextViewResult.flatMap { contextView ->
            // Get all scopes (for total count)
            scopeRepository.findAll().flatMap { allScopes ->
                // TODO: Implement actual filter parsing and evaluation
                // For now, return all scopes as "filtered" result
                val filteredScopes = allScopes
                
                // Apply pagination
                val paginatedScopes = filteredScopes
                    .drop(query.offset)
                    .take(query.limit)
                
                // Map to DTOs
                val scopeResults = paginatedScopes.map { scope ->
                    // Convert Aspects to Map<String, List<String>>
                    val aspectsMap = mutableMapOf<String, List<String>>()
                    scope.aspects.toMap().forEach { (key, values) ->
                        aspectsMap[key.value] = values.map { it.value }
                    }
                    
                    ScopeResult(
                        id = scope.id.value,
                        title = scope.title.value,
                        description = scope.description?.value,
                        parentId = scope.parentId?.value,
                        aspects = aspectsMap,
                        createdAt = scope.createdAt,
                        updatedAt = scope.updatedAt
                    )
                }
                
                val contextViewResult = ContextViewResult(
                    id = contextView.id.value,
                    name = contextView.name.value,
                    filterExpression = contextView.filter.value,
                    description = contextView.description?.value,
                    isActive = activeContextService.getCurrentContext()?.id == contextView.id,
                    createdAt = contextView.createdAt,
                    updatedAt = contextView.updatedAt
                )
                
                FilteredScopesResult(
                    scopes = scopeResults,
                    appliedContext = contextViewResult,
                    totalCount = allScopes.size,
                    filteredCount = filteredScopes.size
                ).right()
            }.mapLeft { error ->
                DomainErrorMapper.mapToApplicationError(error)
            }
        }
    }
}