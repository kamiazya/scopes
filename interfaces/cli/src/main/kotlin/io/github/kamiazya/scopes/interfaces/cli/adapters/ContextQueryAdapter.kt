package io.github.kamiazya.scopes.interfaces.cli.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.context.ContextViewContract
import io.github.kamiazya.scopes.contracts.scopemanagement.context.GetActiveContextQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.context.GetContextViewQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.context.ListContextViewsQuery

/**
 * Query adapter for context-related CLI queries.
 * Maps between CLI queries and context view query port.
 */
class ContextQueryAdapter(private val contextViewQueryPort: ContextViewQueryPort) {
    /**
     * List all context views.
     */
    suspend fun listContexts(request: ListContextViewsQuery): ContextViewContract.ListContextViewsResponse = contextViewQueryPort.listContextViews(request)

    /**
     * Get a specific context view by key.
     */
    suspend fun getContext(request: GetContextViewQuery): ContextViewContract.GetContextViewResponse = contextViewQueryPort.getContextView(request)

    /**
     * Get the currently active context.
     */
    suspend fun getCurrentContext(request: GetActiveContextQuery): ContextViewContract.GetActiveContextResponse = contextViewQueryPort.getActiveContext(request)
}
