package io.github.kamiazya.scopes.contracts.scopemanagement

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetActiveContextQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetContextViewQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListContextViewsQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.types.ContextView

/**
 * Public contract for context view query operations.
 * Provides a stable API for reading context views across bounded contexts.
 */
public interface ContextViewQueryPort {
    /**
     * Lists all context views.
     */
    public suspend fun listContextViews(query: ListContextViewsQuery): Either<ScopeContractError, List<ContextView>>

    /**
     * Gets a specific context view by key.
     * Returns null if not found (not an error case for queries).
     */
    public suspend fun getContextView(query: GetContextViewQuery): Either<ScopeContractError, ContextView?>

    /**
     * Gets the currently active context.
     * Returns null if no active context is set.
     */
    public suspend fun getActiveContext(query: GetActiveContextQuery): Either<ScopeContractError, ContextView?>
}
