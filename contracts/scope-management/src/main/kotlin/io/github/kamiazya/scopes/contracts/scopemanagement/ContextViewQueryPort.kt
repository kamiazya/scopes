package io.github.kamiazya.scopes.contracts.scopemanagement

import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetActiveContextQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetContextViewQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListContextViewsQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.GetActiveContextResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.GetContextViewResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ListContextViewsResult

/**
 * Public contract for context view query operations.
 * Provides a stable API for reading context views across bounded contexts.
 */
public interface ContextViewQueryPort {
    /**
     * Lists all context views.
     */
    public suspend fun listContextViews(query: ListContextViewsQuery): ListContextViewsResult

    /**
     * Gets a specific context view by key.
     */
    public suspend fun getContextView(query: GetContextViewQuery): GetContextViewResult

    /**
     * Gets the currently active context.
     */
    public suspend fun getActiveContext(query: GetActiveContextQuery): GetActiveContextResult
}
