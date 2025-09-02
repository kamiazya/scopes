package io.github.kamiazya.scopes.interfaces.cli.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.context.ContextView
import io.github.kamiazya.scopes.contracts.scopemanagement.context.ContextViewContract
import io.github.kamiazya.scopes.contracts.scopemanagement.context.CreateContextViewRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.DeleteContextViewRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.GetActiveContextRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.GetContextViewRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.ListContextViewsRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.SetActiveContextRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.UpdateContextViewRequest
import io.github.kamiazya.scopes.scopemanagement.application.command.CreateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.UpdateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.context.CreateContextViewUseCase
import io.github.kamiazya.scopes.scopemanagement.application.command.context.DeleteContextViewUseCase
import io.github.kamiazya.scopes.scopemanagement.application.command.context.UpdateContextViewUseCase
import io.github.kamiazya.scopes.scopemanagement.application.dto.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.query.context.GetContextViewUseCase
import io.github.kamiazya.scopes.scopemanagement.application.query.context.ListContextViewsUseCase
import io.github.kamiazya.scopes.scopemanagement.application.service.ActiveContextService
import io.github.kamiazya.scopes.scopemanagement.application.error.ContextError as AppContextError

/**
 * Adapter for context-related CLI commands.
 * Maps between CLI commands and application use cases.
 */
class ContextCommandAdapter(
    private val createContextViewUseCase: CreateContextViewUseCase,
    private val listContextViewsUseCase: ListContextViewsUseCase,
    private val getContextViewUseCase: GetContextViewUseCase,
    private val updateContextViewUseCase: UpdateContextViewUseCase,
    private val deleteContextViewUseCase: DeleteContextViewUseCase,
    private val activeContextService: ActiveContextService,
) {
    /**
     * Create a new context view.
     */
    suspend fun createContext(request: CreateContextViewRequest): ContextViewContract.CreateContextViewResponse {
        val result = createContextViewUseCase.execute(
            CreateContextViewCommand(
                key = request.key,
                name = request.name,
                filter = request.filter,
                description = request.description,
            ),
        )

        return result.fold(
            ifLeft = { error -> mapApplicationErrorToCreateResponse(error) },
            ifRight = { contextViewDto ->
                ContextViewContract.CreateContextViewResponse.Success(
                    contextViewDto.toContractContextView(),
                )
            },
        )
    }

    /**
     * List all context views.
     */
    suspend fun listContexts(request: ListContextViewsRequest): ContextViewContract.ListContextViewsResponse {
        val result = listContextViewsUseCase.execute()

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

    /**
     * Get a specific context view by key.
     */
    suspend fun getContext(request: GetContextViewRequest): ContextViewContract.GetContextViewResponse {
        val result = getContextViewUseCase.execute(request.key)

        return result.fold(
            ifLeft = { error -> mapApplicationErrorToGetResponse(error, request.key) },
            ifRight = { contextViewDto ->
                ContextViewContract.GetContextViewResponse.Success(
                    contextViewDto.toContractContextView(),
                )
            },
        )
    }

    /**
     * Update an existing context view.
     */
    suspend fun updateContext(request: UpdateContextViewRequest): ContextViewContract.UpdateContextViewResponse {
        val result = updateContextViewUseCase.execute(
            UpdateContextViewCommand(
                key = request.key,
                name = request.name,
                filter = request.filter,
                description = request.description,
            ),
        )

        return result.fold(
            ifLeft = { error -> mapApplicationErrorToUpdateResponse(error, request.key) },
            ifRight = { contextViewDto ->
                ContextViewContract.UpdateContextViewResponse.Success(
                    contextViewDto.toContractContextView(),
                )
            },
        )
    }

    /**
     * Delete a context view.
     */
    suspend fun deleteContext(request: DeleteContextViewRequest): ContextViewContract.DeleteContextViewResponse {
        val result = deleteContextViewUseCase.execute(request.key)

        return result.fold(
            ifLeft = { error -> mapApplicationErrorToDeleteResponse(error, request.key) },
            ifRight = { ContextViewContract.DeleteContextViewResponse.Success },
        )
    }

    /**
     * Get the currently active context.
     */
    suspend fun getCurrentContext(request: GetActiveContextRequest): ContextViewContract.GetActiveContextResponse {
        val contextView = activeContextService.getCurrentContext()

        return ContextViewContract.GetActiveContextResponse.Success(
            contextView?.let { domainContextView ->
                ContextView(
                    key = domainContextView.key.value,
                    name = domainContextView.name.value,
                    filter = domainContextView.filter.expression,
                    description = domainContextView.description?.value,
                    createdAt = domainContextView.createdAt,
                    updatedAt = domainContextView.updatedAt,
                )
            },
        )
    }

    /**
     * Set a context as the current active context.
     */
    suspend fun setCurrentContext(request: SetActiveContextRequest): ContextViewContract.SetActiveContextResponse {
        val result = activeContextService.switchToContextByKey(request.key)

        return result.fold(
            ifLeft = { _ -> ContextViewContract.SetActiveContextResponse.NotFound(request.key) },
            ifRight = { ContextViewContract.SetActiveContextResponse.Success },
        )
    }

    /**
     * Clear the current active context.
     */
    suspend fun clearCurrentContext(): ContextViewContract.SetActiveContextResponse {
        val result = activeContextService.clearActiveContext()

        return result.fold(
            ifLeft = { _ ->
                // Clear errors are rare
                ContextViewContract.SetActiveContextResponse.Success
            },
            ifRight = { ContextViewContract.SetActiveContextResponse.Success },
        )
    }

    private fun mapApplicationErrorToCreateResponse(error: ApplicationError): ContextViewContract.CreateContextViewResponse = when (error) {
        is AppContextError.InvalidFilter -> ContextViewContract.CreateContextViewResponse.InvalidFilter(error.filter, error.reason)
        is AppContextError.DuplicateContextKey -> ContextViewContract.CreateContextViewResponse.DuplicateKey(error.key)
        else -> ContextViewContract.CreateContextViewResponse.InvalidFilter("", "Unknown error: ${error.javaClass.simpleName}")
    }

    private fun mapApplicationErrorToGetResponse(error: ApplicationError, key: String): ContextViewContract.GetContextViewResponse = when (error) {
        is AppContextError.ContextNotFound -> ContextViewContract.GetContextViewResponse.NotFound(key)
        else -> ContextViewContract.GetContextViewResponse.NotFound(key) // Fallback
    }

    private fun mapApplicationErrorToUpdateResponse(error: ApplicationError, key: String): ContextViewContract.UpdateContextViewResponse = when (error) {
        is AppContextError.InvalidFilter -> ContextViewContract.UpdateContextViewResponse.InvalidFilter(error.filter, error.reason)
        is AppContextError.ContextNotFound -> ContextViewContract.UpdateContextViewResponse.NotFound(key)
        else -> ContextViewContract.UpdateContextViewResponse.NotFound(key) // Fallback
    }

    private fun mapApplicationErrorToDeleteResponse(error: ApplicationError, key: String): ContextViewContract.DeleteContextViewResponse = when (error) {
        is AppContextError.ContextNotFound -> ContextViewContract.DeleteContextViewResponse.NotFound(key)
        else -> ContextViewContract.DeleteContextViewResponse.NotFound(key) // Fallback
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
