package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.context.ContextView
import io.github.kamiazya.scopes.contracts.scopemanagement.context.ContextViewContract
import io.github.kamiazya.scopes.contracts.scopemanagement.context.GetActiveContextQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.context.GetContextViewQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.context.ListContextViewsQuery
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

    override suspend fun listContextViews(query: ListContextViewsQuery): ContextViewContract.ListContextViewsResponse {
        val result = listContextViewsHandler(ListContextViews())

        return result.fold(
            ifLeft = { _ ->
                // List errors are rare, but handle if needed
                ContextViewContract.ListContextViewsResponse.Success(emptyList())
            },
            ifRight = { contextViewDtos ->
                ContextViewContract.ListContextViewsResponse.Success(
                    contextViewDtos.map { it.toContractContextView() },
                )
            },
        )
    }

    override suspend fun getContextView(query: GetContextViewQuery): ContextViewContract.GetContextViewResponse {
        val result = getContextViewHandler(GetContextView(query.key))

        return result.fold(
            ifLeft = { error -> mapScopesErrorToGetResponse(error, query.key) },
            ifRight = { contextViewDto ->
                if (contextViewDto != null) {
                    ContextViewContract.GetContextViewResponse.Success(
                        contextViewDto.toContractContextView(),
                    )
                } else {
                    ContextViewContract.GetContextViewResponse.NotFound(query.key)
                }
            },
        )
    }

    override suspend fun getActiveContext(query: GetActiveContextQuery): ContextViewContract.GetActiveContextResponse {
        val domainContextView = activeContextService.getCurrentContext()

        return ContextViewContract.GetActiveContextResponse.Success(
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

    private fun mapScopesErrorToGetResponse(error: ScopesError, key: String): ContextViewContract.GetContextViewResponse = when (error) {
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError.NotFound -> ContextViewContract.GetContextViewResponse.NotFound(key)
        else -> ContextViewContract.GetContextViewResponse.NotFound(key)
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
