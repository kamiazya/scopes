package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetActiveContextQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetContextViewQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListContextViewsQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.types.ContextView

/**
 * Query adapter for context-related CLI queries.
 * Maps between CLI queries and context view query port.
 */
class ContextQueryAdapter(private val contextViewQueryPort: ContextViewQueryPort) {
    /**
     * List all context views.
     */
    suspend fun listContextViews(): Either<ScopeContractError, List<ContextView>> = contextViewQueryPort.listContextViews(ListContextViewsQuery)

    /**
     * Get a specific context view by key.
     */
    suspend fun getContextView(key: String): Either<ScopeContractError, ContextView?> = contextViewQueryPort.getContextView(GetContextViewQuery(key))

    /**
     * Get the currently active context.
     */
    suspend fun getCurrentContext(): Either<ScopeContractError, ContextView?> = contextViewQueryPort.getActiveContext(GetActiveContextQuery)
}
