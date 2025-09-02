package io.github.kamiazya.scopes.scopemanagement.application.query

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.application.dto.ContextViewResult
import io.github.kamiazya.scopes.scopemanagement.application.dto.FilteredScopesResult
import io.github.kamiazya.scopes.scopemanagement.application.dto.ScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ContextViewMapper
import io.github.kamiazya.scopes.scopemanagement.application.service.ContextAuditService
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ActiveContextRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.FilterEvaluationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey

/**
 * Use case for filtering scopes based on a context view.
 * Applies the filter defined in a context view to retrieve matching scopes.
 */
class GetFilteredScopesUseCase(
    private val scopeRepository: ScopeRepository,
    private val contextViewRepository: ContextViewRepository,
    private val activeContextRepository: ActiveContextRepository,
    private val aspectDefinitionRepository: AspectDefinitionRepository,
    private val filterEvaluationService: FilterEvaluationService = FilterEvaluationService(),
    private val contextAuditService: ContextAuditService,
    private val contextViewMapper: ContextViewMapper = ContextViewMapper(),
) : UseCase<GetFilteredScopesQuery, ScopesError, FilteredScopesResult> {

    override suspend operator fun invoke(input: GetFilteredScopesQuery): Either<ScopesError, FilteredScopesResult> {
        // Get the context view to use
        val contextView: ContextView? = when {
            input.contextKey != null -> {
                // Create ContextViewKey from string
                val contextViewKey = ContextViewKey.create(input.contextKey).fold(
                    { return ScopesError.InvalidOperation("Invalid context key: ${input.contextKey}").left() },
                    { it },
                )

                // Use specified context
                contextViewRepository.findByKey(contextViewKey).fold(
                    { return ScopesError.SystemError("Failed to find context view by key: $it").left() },
                    { it },
                )
            }
            else -> {
                // Use active context if available
                activeContextRepository.getActiveContext().fold(
                    { return it.left() },
                    { it },
                )
            }
        }

        // Get total count first (more efficient than loading all scopes)
        val totalCount = scopeRepository.countAll().fold(
            { return it.left() },
            { it },
        )

        // Get scopes with pagination
        val allScopes = scopeRepository.findAll(input.offset, input.limit).fold(
            { return it.left() },
            { it },
        )

        // Apply filter if we have a context view
        val filteredScopes = if (contextView != null) {
            // Get aspect definitions for type-aware comparison
            val aspectDefinitions = aspectDefinitionRepository.findAll().fold(
                { return ScopesError.SystemError("Failed to load aspect definitions: $it").left() },
                { it.associateBy { def -> def.key.value } },
            )

            // Use the domain-rich filter method
            val filtered = contextView.filterScopes(allScopes, aspectDefinitions, filterEvaluationService).fold(
                { return ScopesError.SystemError("Failed to apply filter: $it").left() },
                { it },
            )

            // Publish audit event for context usage (non-blocking)
            contextAuditService.publishContextApplied(
                contextView = contextView,
                scopeCount = filtered.size,
                totalScopeCount = totalCount,
                appliedBy = null, // TODO: Add user context to track who applied the filter
            ).fold(
                { error ->
                    // TODO: Add proper logging - for now, silently continue
                    // logger.warn("Failed to publish context applied event: $error")
                },
                { },
            )

            filtered
        } else {
            // No filter applied
            allScopes
        }

        // Map to DTOs
        val scopeResults = filteredScopes.map { scope ->
            toScopeResult(scope)
        }

        val contextViewResult = contextView?.let { cv ->
            // Check if this is the active context
            val activeContextId = activeContextRepository.getActiveContext().fold(
                { null },
                { it?.id },
            )

            toContextViewResult(cv, isActive = cv.id == activeContextId)
        }

        return FilteredScopesResult(
            scopes = scopeResults,
            appliedContext = contextViewResult,
            totalCount = totalCount,
            filteredCount = filteredScopes.size,
        ).right()
    }
}

// Helper functions for mapping
private fun toScopeResult(scope: Scope): ScopeResult = ScopeResult(
    id = scope.id.value,
    title = scope.title.value,
    description = scope.description?.value,
    parentId = scope.parentId?.value,
    aspects = scope.aspects.toMap().mapKeys { it.key.value }
        .mapValues { it.value.map { v -> v.value } },
    createdAt = scope.createdAt,
    updatedAt = scope.updatedAt,
)

private fun toContextViewResult(contextView: ContextView, isActive: Boolean): ContextViewResult = ContextViewResult(
    id = contextView.id.value,
    key = contextView.key.value,
    name = contextView.name.value,
    filterExpression = contextView.filter.expression,
    description = contextView.description?.value,
    isActive = isActive,
    createdAt = contextView.createdAt,
    updatedAt = contextView.updatedAt,
)
