package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetActiveContextQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetContextViewQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListContextViewsQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.GetActiveContextResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.GetContextViewResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ListContextViewsResult
import io.github.kamiazya.scopes.contracts.scopemanagement.types.ContextView
import io.github.kamiazya.scopes.scopemanagement.application.dto.context.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetContextView
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.ListContextViews
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.context.GetContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.context.ListContextViewsHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.ActiveContextService
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError

/**
 * Query port adapter implementation for ContextView operations.
 * Handles query operations that read context view data.
 */
public class ContextViewQueryPortAdapter(
    private val listContextViewsHandler: ListContextViewsHandler,
    private val getContextViewHandler: GetContextViewHandler,
    private val activeContextService: ActiveContextService,
) : ContextViewQueryPort {

    override suspend fun listContextViews(query: ListContextViewsQuery): ListContextViewsResult {
        val result = listContextViewsHandler(ListContextViews())

        return result.fold(
            ifLeft = { _ ->
                // List errors are rare, but handle if needed
                ListContextViewsResult.Success(emptyList())
            },
            ifRight = { contextViewDtos ->
                ListContextViewsResult.Success(
                    contextViewDtos.map { it.toContractContextView() },
                )
            },
        )
    }

    override suspend fun getContextView(query: GetContextViewQuery): GetContextViewResult {
        val result = getContextViewHandler(GetContextView(query.key))

        return result.fold(
            ifLeft = { error -> mapScopesErrorToGetResponse(error, query.key) },
            ifRight = { contextViewDto ->
                if (contextViewDto != null) {
                    GetContextViewResult.Success(
                        contextViewDto.toContractContextView(),
                    )
                } else {
                    GetContextViewResult.NotFound(query.key)
                }
            },
        )
    }

    override suspend fun getActiveContext(query: GetActiveContextQuery): GetActiveContextResult {
        val domainContextView = activeContextService.getCurrentContext()

        return GetActiveContextResult.Success(
            domainContextView?.let {
                ContextView(
                    key = it.key.value,
                    name = it.name.value,
                    filter = it.filter.expression,
                    description = it.description?.value,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                )
            },
        )
    }

    private fun mapScopesErrorToGetResponse(error: ScopesError, key: String): GetContextViewResult = when (error) {
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError.NotFound -> GetContextViewResult.NotFound(key)
        else -> GetContextViewResult.NotFound(key)
    }

    private fun ContextViewDto.toContractContextView(): ContextView = ContextView(
        key = this.key,
        name = this.name,
        filter = this.filter,
        description = this.description,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )
}
