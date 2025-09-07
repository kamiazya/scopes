package io.github.kamiazya.scopes.interfaces.cli.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetActiveContextQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetContextViewQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListContextViewsQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.GetActiveContextResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.GetContextViewResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ListContextViewsResult

/**
 * Query adapter for context-related CLI queries.
 * Maps between CLI queries and context view query port.
 */
class ContextQueryAdapter(private val contextViewQueryPort: ContextViewQueryPort) {
    /**
     * List all context views.
     */
    suspend fun listContexts(request: ListContextViewsQuery): ListContextViewsResult = contextViewQueryPort.listContextViews(request)

    /**
     * Get a specific context view by key.
     */
    suspend fun getContext(request: GetContextViewQuery): GetContextViewResult = contextViewQueryPort.getContextView(request)

    /**
     * Get the currently active context.
     */
    suspend fun getCurrentContext(request: GetActiveContextQuery): GetActiveContextResult = contextViewQueryPort.getActiveContext(request)
}
