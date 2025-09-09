package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetActiveContextQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetContextViewQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListContextViewsQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.types.ContextView
import io.github.kamiazya.scopes.scopemanagement.application.dto.context.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetContextView
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.ListContextViews
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.context.GetContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.context.ListContextViewsHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.ActiveContextService
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError
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

    override suspend fun listContextViews(query: ListContextViewsQuery): Either<ScopeContractError, List<ContextView>> {
        val result = listContextViewsHandler(ListContextViews())

        return result.fold(
            ifLeft = { error -> mapScopesErrorToScopeContractError(error).left() },
            ifRight = { contextViewDtos ->
                contextViewDtos.map { it.toContractContextView() }.right()
            },
        )
    }

    override suspend fun getContextView(query: GetContextViewQuery): Either<ScopeContractError, ContextView?> {
        val result = getContextViewHandler(GetContextView(query.key))

        return result.fold(
            ifLeft = { error -> mapScopesErrorToScopeContractError(error).left() },
            ifRight = { contextViewDto ->
                contextViewDto?.toContractContextView().right()
            },
        )
    }

    override suspend fun getActiveContext(query: GetActiveContextQuery): Either<ScopeContractError, ContextView?> {
        return activeContextService.getCurrentContext().fold(
            ifLeft = { error -> mapApplicationErrorToContractError(error).left() },
            ifRight = { domainContextView ->
                domainContextView?.let {
                    ContextView(
                        key = it.key.value,
                        name = it.name.value,
                        filter = it.filter.expression,
                        description = it.description?.value,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                    )
                }.right()
            }
        )
    }

    /**
     * Maps domain errors to contract layer errors for query operations.
     */
    private fun mapScopesErrorToScopeContractError(error: ScopesError): ScopeContractError = when (error) {
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError.NotFound ->
            ScopeContractError.BusinessError.NotFound(error.identifier)
        else -> ScopeContractError.SystemError.ServiceUnavailable("ContextViewService")
    }

    /**
     * Maps application errors to contract layer errors for query operations.
     */
    private fun mapApplicationErrorToContractError(error: ApplicationError): ScopeContractError = when (error) {
        is io.github.kamiazya.scopes.scopemanagement.application.error.ContextError.StateNotFound ->
            ScopeContractError.BusinessError.NotFound(error.contextId)
        else -> ScopeContractError.SystemError.ServiceUnavailable("ActiveContextService")
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
