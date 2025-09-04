package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewPort
import io.github.kamiazya.scopes.contracts.scopemanagement.context.ContextView
import io.github.kamiazya.scopes.contracts.scopemanagement.context.ContextViewContract
import io.github.kamiazya.scopes.contracts.scopemanagement.context.CreateContextViewRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.DeleteContextViewRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.GetActiveContextRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.GetContextViewRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.ListContextViewsRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.SetActiveContextRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.UpdateContextViewRequest
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.CreateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.DeleteContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.UpdateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.context.CreateContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.context.DeleteContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.context.UpdateContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.dto.context.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetContextView
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.ListContextViews
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.context.GetContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.context.ListContextViewsHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.ActiveContextService
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError

/**
 * Adapter implementation for ContextViewPort.
 * Maps between contract layer requests/responses and application layer handlers.
 */
public class ContextViewPortAdapter(
    private val createContextViewHandler: CreateContextViewHandler,
    private val listContextViewsHandler: ListContextViewsHandler,
    private val getContextViewHandler: GetContextViewHandler,
    private val updateContextViewHandler: UpdateContextViewHandler,
    private val deleteContextViewHandler: DeleteContextViewHandler,
    private val activeContextService: ActiveContextService,
) : ContextViewPort {

    override suspend fun createContextView(request: CreateContextViewRequest): ContextViewContract.CreateContextViewResponse {
        val result = createContextViewHandler(
            CreateContextViewCommand(
                key = request.key,
                name = request.name,
                filter = request.filter,
                description = request.description,
            ),
        )

        return result.fold(
            ifLeft = { error -> mapScopesErrorToCreateResponse(error) },
            ifRight = { contextViewDto ->
                ContextViewContract.CreateContextViewResponse.Success(
                    contextViewDto.toContractContextView(),
                )
            },
        )
    }

    override suspend fun listContextViews(request: ListContextViewsRequest): ContextViewContract.ListContextViewsResponse {
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

    override suspend fun getContextView(request: GetContextViewRequest): ContextViewContract.GetContextViewResponse {
        val result = getContextViewHandler(GetContextView(request.key))

        return result.fold(
            ifLeft = { error -> mapScopesErrorToGetResponse(error, request.key) },
            ifRight = { contextViewDto ->
                if (contextViewDto != null) {
                    ContextViewContract.GetContextViewResponse.Success(
                        contextViewDto.toContractContextView(),
                    )
                } else {
                    ContextViewContract.GetContextViewResponse.NotFound(request.key)
                }
            },
        )
    }

    override suspend fun updateContextView(request: UpdateContextViewRequest): ContextViewContract.UpdateContextViewResponse {
        val result = updateContextViewHandler(
            UpdateContextViewCommand(
                key = request.key,
                name = request.name,
                filter = request.filter,
                description = request.description,
            ),
        )

        return result.fold(
            ifLeft = { error -> mapScopesErrorToUpdateResponse(error, request.key) },
            ifRight = { contextViewDto ->
                ContextViewContract.UpdateContextViewResponse.Success(
                    contextViewDto.toContractContextView(),
                )
            },
        )
    }

    override suspend fun deleteContextView(request: DeleteContextViewRequest): ContextViewContract.DeleteContextViewResponse {
        val result = deleteContextViewHandler(DeleteContextViewCommand(request.key))

        return result.fold(
            ifLeft = { error -> mapScopesErrorToDeleteResponse(error, request.key) },
            ifRight = { ContextViewContract.DeleteContextViewResponse.Success },
        )
    }

    override suspend fun getActiveContext(request: GetActiveContextRequest): ContextViewContract.GetActiveContextResponse {
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

    override suspend fun setActiveContext(request: SetActiveContextRequest): ContextViewContract.SetActiveContextResponse {
        val result = activeContextService.switchToContextByKey(request.key)

        return result.fold(
            ifLeft = { _ -> ContextViewContract.SetActiveContextResponse.NotFound(request.key) },
            ifRight = { ContextViewContract.SetActiveContextResponse.Success },
        )
    }

    override suspend fun clearActiveContext(): ContextViewContract.SetActiveContextResponse {
        val result = activeContextService.clearActiveContext()

        return result.fold(
            ifLeft = { _ ->
                // Clear errors are rare
                ContextViewContract.SetActiveContextResponse.Success
            },
            ifRight = { ContextViewContract.SetActiveContextResponse.Success },
        )
    }

    private fun mapScopesErrorToCreateResponse(error: ScopesError): ContextViewContract.CreateContextViewResponse = when (error) {
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError.InvalidFilter,
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError.InvalidFilterSyntax,
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError.ValidationFailed,
        ->
            ContextViewContract.CreateContextViewResponse.InvalidFilter("", error.toString())
        else -> ContextViewContract.CreateContextViewResponse.InvalidFilter("", error.toString())
    }

    private fun mapScopesErrorToGetResponse(error: ScopesError, key: String): ContextViewContract.GetContextViewResponse = when (error) {
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError.NotFound -> ContextViewContract.GetContextViewResponse.NotFound(key)
        else -> ContextViewContract.GetContextViewResponse.NotFound(key)
    }

    private fun mapScopesErrorToUpdateResponse(error: ScopesError, key: String): ContextViewContract.UpdateContextViewResponse = when (error) {
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError.InvalidFilter,
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError.InvalidFilterSyntax,
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError.ValidationFailed,
        ->
            ContextViewContract.UpdateContextViewResponse.InvalidFilter("", error.toString())
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError.NotFound -> ContextViewContract.UpdateContextViewResponse.NotFound(key)
        else -> ContextViewContract.UpdateContextViewResponse.NotFound(key)
    }

    private fun mapScopesErrorToDeleteResponse(error: ScopesError, key: String): ContextViewContract.DeleteContextViewResponse = when (error) {
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError.NotFound -> ContextViewContract.DeleteContextViewResponse.NotFound(key)
        else -> ContextViewContract.DeleteContextViewResponse.NotFound(key)
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
