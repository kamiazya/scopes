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
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.context.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetContextView
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.ListContextViews
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.context.GetContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.context.ListContextViewsHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.ActiveContextService

/**
 * Query port adapter implementation for ContextView operations.
 * Handles query operations that read context view data.
 */
public class ContextViewQueryPortAdapter(
    private val listContextViewsHandler: ListContextViewsHandler,
    private val getContextViewHandler: GetContextViewHandler,
    private val activeContextService: ActiveContextService,
    logger: Logger,
) : ContextViewQueryPort {
    private val errorMapper = ApplicationErrorMapper(logger)

    override suspend fun listContextViews(query: ListContextViewsQuery): Either<ScopeContractError, List<ContextView>> {
        val result = listContextViewsHandler(ListContextViews())

        return result.fold(
            ifLeft = { error -> errorMapper.mapToContractError(error).left() },
            ifRight = { contextViewDtos ->
                contextViewDtos.map { it.toContractContextView() }.right()
            },
        )
    }

    override suspend fun getContextView(query: GetContextViewQuery): Either<ScopeContractError, ContextView?> {
        val result = getContextViewHandler(GetContextView(query.key))

        return result.fold(
            ifLeft = { error -> errorMapper.mapToContractError(error).left() },
            ifRight = { contextViewDto ->
                contextViewDto?.toContractContextView().right()
            },
        )
    }

    override suspend fun getActiveContext(query: GetActiveContextQuery): Either<ScopeContractError, ContextView?> {
        val domainContextView = activeContextService.getCurrentContext()

        return domainContextView?.let {
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

    private fun ContextViewDto.toContractContextView(): ContextView = ContextView(
        key = this.key,
        name = this.name,
        filter = this.filter,
        description = this.description,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )
}
