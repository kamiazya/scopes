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
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.command.GetFilteredScopesQuery
import io.github.kamiazya.scopes.domain.entity.ContextView
import io.github.kamiazya.scopes.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.valueobject.ContextName
import io.github.kamiazya.scopes.domain.entity.Scope

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
) : UseCase<GetFilteredScopesQuery, ApplicationError, FilteredScopesResult> {

    override suspend operator fun invoke(input: GetFilteredScopesQuery): Either<ApplicationError, FilteredScopesResult> {
        // Resolve the context view
        val contextViewResult: Either<ApplicationError, ContextView> = if (input.contextName != null) {
            // Find by name
            ContextName.create(input.contextName).mapLeft { errorMessage ->
                ApplicationError.ContextError.NamingInvalidFormat(
                    attemptedName = input.contextName
                )
            }.flatMap { contextName ->
                contextViewRepository.findByName(contextName).mapLeft { error ->
                    DomainErrorMapper.mapToApplicationError(error)
                }.flatMap { contextView ->
                    contextView?.right() ?: ApplicationError.ContextError.StateNotFound(
                        contextName = input.contextName
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
                // Parse and apply the filter expression
                val filteredScopes = filterScopes(allScopes, contextView.filter.value)

                // Apply pagination
                val paginatedScopes = filteredScopes
                    .drop(input.offset)
                    .take(input.limit)

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

    /**
     * Simple filter parser and evaluator.
     * Supports basic filters like:
     * - status:active
     * - type:task
     * - priority:high
     * - Combined with AND/OR
     */
    private fun filterScopes(scopes: List<Scope>, filterExpression: String): List<Scope> {
        if (filterExpression.isBlank() || filterExpression == "all") {
            return scopes
        }

        // Parse simple key:value filters
        val filters = filterExpression.split(" AND ", " OR ", ",")
            .map { it.trim() }
            .filter { it.contains(':') }
            .map { filter ->
                val parts = filter.split(':')
                if (parts.size == 2) {
                    parts[0].trim() to parts[1].trim()
                } else {
                    null
                }
            }
            .filterNotNull()

        if (filters.isEmpty()) {
            return scopes
        }

        // Apply filters
        return scopes.filter { scope ->
            filters.any { (key, value) ->
                // Check if the scope has this aspect with the specified value
                val aspectKey = scope.aspects.keys().find { it.value == key }
                if (aspectKey != null) {
                    val aspectValues = scope.aspects.get(aspectKey)
                    aspectValues?.any { it.value.equals(value, ignoreCase = true) } == true
                } else {
                    // Check special properties
                    when (key.lowercase()) {
                        "title" -> scope.title.value.contains(value, ignoreCase = true)
                        "description" -> scope.description?.value?.contains(value, ignoreCase = true) == true
                        "has_parent" -> (value == "true" && scope.parentId != null) || (value == "false" && scope.parentId == null)
                        else -> false
                    }
                }
            }
        }
    }
}
