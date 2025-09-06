package io.github.kamiazya.scopes.contracts.scopemanagement

import io.github.kamiazya.scopes.contracts.scopemanagement.context.ContextViewContract
import io.github.kamiazya.scopes.contracts.scopemanagement.context.GetActiveContextQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.context.GetContextViewQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.context.ListContextViewsQuery

/**
 * Public contract for context view query operations.
 * Provides a stable API for reading context views across bounded contexts.
 */
public interface ContextViewQueryPort {
    /**
     * Lists all context views.
     */
    public suspend fun listContextViews(query: ListContextViewsQuery): ContextViewContract.ListContextViewsResponse

    /**
     * Gets a specific context view by key.
     */
    public suspend fun getContextView(query: GetContextViewQuery): ContextViewContract.GetContextViewResponse

    /**
     * Gets the currently active context.
     */
    public suspend fun getActiveContext(query: GetActiveContextQuery): ContextViewContract.GetActiveContextResponse
}
