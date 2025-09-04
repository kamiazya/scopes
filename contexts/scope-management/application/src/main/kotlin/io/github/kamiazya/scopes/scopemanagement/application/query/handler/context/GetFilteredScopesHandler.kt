package io.github.kamiazya.scopes.scopemanagement.application.query.handler.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ContextViewResult
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.FilteredScopesResult
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetFilteredScopes
import io.github.kamiazya.scopes.scopemanagement.application.service.ContextAuditService
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ActiveContextRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.filter.FilterEvaluationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey

/**
 * Handler for filtering scopes based on a context view.
 * Applies the filter defined in a context view to retrieve matching scopes.
 */
class GetFilteredScopesHandler(
    private val scopeRepository: ScopeRepository,
    private val contextViewRepository: ContextViewRepository,
    private val activeContextRepository: ActiveContextRepository,
    private val aspectDefinitionRepository: AspectDefinitionRepository,
    private val contextAuditService: ContextAuditService,
    private val transactionManager: TransactionManager,
    private val filterEvaluationService: FilterEvaluationService = FilterEvaluationService(),
) : QueryHandler<GetFilteredScopes, ScopesError, FilteredScopesResult> {

    override suspend operator fun invoke(query: GetFilteredScopes): Either<ScopesError, FilteredScopesResult> = transactionManager.inTransaction {
        either {
            // Get the context view to use
            val contextView: ContextView? = when {
                query.contextKey != null -> {
                    // Create ContextViewKey from string
                    val contextViewKey = ContextViewKey.create(query.contextKey).bind()

                    // Use specified context
                    contextViewRepository.findByKey(contextViewKey)
                        .mapLeft { ScopesError.SystemError("Failed to find context view: $it") }
                        .bind()
                }
                else -> {
                    // Use active context if available
                    activeContextRepository.getActiveContext()
                        .mapLeft { ScopesError.SystemError("Failed to get active context: $it") }
                        .bind()
                }
            }

            // Get all scopes with pagination
            val allScopes = scopeRepository.findAll(query.offset, query.limit)
                .mapLeft { ScopesError.SystemError("Failed to find scopes: $it") }
                .bind()

            // Get total count - use findAll() and count since there's no count() method
            val totalCount = allScopes.size

            // Apply filter if we have a context view
            val filteredScopes = if (contextView != null) {
                // Get aspect definitions for type-aware comparison
                val aspectDefinitions = aspectDefinitionRepository.findAll()
                    .mapLeft { ScopesError.SystemError("Failed to load aspect definitions: $it") }
                    .bind()
                    .associateBy { def -> def.key.value }

                // Use the domain-rich filter method
                val filtered = contextView.filterScopes(allScopes, aspectDefinitions, filterEvaluationService)
                    .mapLeft { ScopesError.SystemError("Failed to apply filter: $it") }
                    .bind()

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

            FilteredScopesResult(
                scopes = scopeResults,
                appliedContext = contextViewResult,
                totalCount = totalCount,
                filteredCount = filteredScopes.size,
            )
        }
    }

    private fun toScopeResult(scope: Scope): ScopeResult = ScopeResult(
        id = scope.id.value.toString(),
        title = scope.title.value,
        description = scope.description?.value,
        parentId = scope.parentId?.value?.toString(),
        aspects = scope.aspects.toMap().mapKeys { it.key.value }
            .mapValues { it.value.toList().map { v -> v.value } },
        createdAt = scope.createdAt,
        updatedAt = scope.updatedAt,
    )

    private fun toContextViewResult(contextView: ContextView, isActive: Boolean): ContextViewResult = ContextViewResult(
        id = contextView.id.value.toString(),
        key = contextView.key.value,
        name = contextView.name.value,
        filterExpression = contextView.filter.expression,
        description = contextView.description?.value,
        isActive = isActive,
        createdAt = contextView.createdAt,
        updatedAt = contextView.updatedAt,
    )
}
