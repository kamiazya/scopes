package io.github.kamiazya.scopes.contracts.scopemanagement

import io.github.kamiazya.scopes.contracts.scopemanagement.context.ContextViewContract
import io.github.kamiazya.scopes.contracts.scopemanagement.context.CreateContextViewRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.DeleteContextViewRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.GetActiveContextRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.GetContextViewRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.ListContextViewsRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.SetActiveContextRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.UpdateContextViewRequest

/**
 * Public contract for context view operations.
 * Provides a stable API for managing context views across bounded contexts.
 */
public interface ContextViewPort {
    /**
     * Creates a new context view.
     */
    public suspend fun createContextView(request: CreateContextViewRequest): ContextViewContract.CreateContextViewResponse

    /**
     * Lists all context views.
     */
    public suspend fun listContextViews(request: ListContextViewsRequest): ContextViewContract.ListContextViewsResponse

    /**
     * Gets a specific context view by key.
     */
    public suspend fun getContextView(request: GetContextViewRequest): ContextViewContract.GetContextViewResponse

    /**
     * Updates an existing context view.
     */
    public suspend fun updateContextView(request: UpdateContextViewRequest): ContextViewContract.UpdateContextViewResponse

    /**
     * Deletes a context view.
     */
    public suspend fun deleteContextView(request: DeleteContextViewRequest): ContextViewContract.DeleteContextViewResponse

    /**
     * Gets the currently active context.
     */
    public suspend fun getActiveContext(request: GetActiveContextRequest): ContextViewContract.GetActiveContextResponse

    /**
     * Sets a context as the current active context.
     */
    public suspend fun setActiveContext(request: SetActiveContextRequest): ContextViewContract.SetActiveContextResponse

    /**
     * Clears the current active context.
     */
    public suspend fun clearActiveContext(): ContextViewContract.SetActiveContextResponse
}
