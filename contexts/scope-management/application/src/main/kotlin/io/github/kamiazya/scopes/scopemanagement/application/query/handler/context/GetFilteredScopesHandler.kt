package io.github.kamiazya.scopes.scopemanagement.application.query.handler.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
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
    private val logger: Logger,
    private val filterEvaluationService: FilterEvaluationService = FilterEvaluationService(),
) : QueryHandler<GetFilteredScopes, ScopesError, FilteredScopesResult> {

    override suspend operator fun invoke(query: GetFilteredScopes): Either<ScopesError, FilteredScopesResult> = transactionManager.inReadOnlyTransaction {
        logger.debug(
            "Getting filtered scopes",
            mapOf(
                "contextKey" to (query.contextKey ?: "active"),
                "offset" to query.offset,
                "limit" to query.limit,
            ),
        )
        either {
            // Get the context view to use
            val contextView: ContextView? = when {
                query.contextKey != null -> {
                    // Create ContextViewKey from string
                    val contextViewKey = ContextViewKey.create(query.contextKey).bind()

                    // Use specified context
                    contextViewRepository.findByKey(contextViewKey)
                        .mapLeft { error ->
                            ScopesError.SystemError(
                                errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                                service = "context-repository",
                                cause = error as? Throwable,
                                context = mapOf("operation" to "find-context-view", "key" to query.contextKey),
                            )
                        }
                        .bind()
                }
                else -> {
                    // Use active context if available
                    activeContextRepository.getActiveContext()
                        .mapLeft { error ->
                            ScopesError.SystemError(
                                errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                                service = "active-context-repository",
                                cause = error as? Throwable,
                                context = mapOf("operation" to "get-active-context"),
                            )
                        }
                        .bind()
                }
            }

            // Get all scopes with pagination
            val allScopes = scopeRepository.findAll(query.offset, query.limit)
                .mapLeft { error ->
                    ScopesError.SystemError(
                        errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                        service = "scope-repository",
                        cause = error as? Throwable,
                        context = mapOf("operation" to "find-all-scopes", "offset" to query.offset, "limit" to query.limit),
                    )
                }
                .bind()

            // Get total count - use findAll() and count since there's no count() method
            val totalCount = allScopes.size

            // Apply filter if we have a context view
            val filteredScopes = if (contextView != null) {
                // Get aspect definitions for type-aware comparison
                val aspectDefinitions = aspectDefinitionRepository.findAll()
                    .mapLeft { error ->
                        ScopesError.SystemError(
                            errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                            service = "aspect-repository",
                            cause = error as? Throwable,
                            context = mapOf("operation" to "load-aspect-definitions"),
                        )
                    }
                    .bind()
                    .associateBy { def -> def.key.value }

                // Use the domain-rich filter method
                val filtered = contextView.filterScopes(allScopes, aspectDefinitions, filterEvaluationService)
                    .mapLeft { error ->
                        ScopesError.SystemError(
                            errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                            service = "filter-evaluation",
                            cause = error as? Throwable,
                            context = mapOf("operation" to "apply-filter", "filter" to contextView.filter.expression),
                        )
                    }
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

            val result = FilteredScopesResult(
                scopes = scopeResults,
                appliedContext = contextViewResult,
                totalCount = totalCount,
                filteredCount = filteredScopes.size,
            )

            logger.info(
                "Successfully filtered scopes",
                mapOf(
                    "contextKey" to (contextView?.key?.value ?: "none"),
                    "totalScopes" to totalCount,
                    "filteredScopes" to filteredScopes.size,
                    "offset" to query.offset,
                    "limit" to query.limit,
                ),
            )

            result
        }
    }.onLeft { error ->
        logger.error(
            "Failed to get filtered scopes",
            mapOf(
                "contextKey" to (query.contextKey ?: "active"),
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                "message" to error.toString(),
            ),
        )
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
