package io.github.kamiazya.scopes.contracts.scopemanagement

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.context.CreateContextViewRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.DeleteContextViewRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.SetActiveContextRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.UpdateContextViewRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError

/**
 * Public contract for context view command operations.
 * Following CQRS principles, this port handles only operations that modify state.
 * All operations return Either for explicit error handling.
 */
public interface ContextViewCommandPort {
    /**
     * Creates a new context view.
     * Returns minimal success indicator following CQRS principles.
     */
    public suspend fun createContextView(command: CreateContextViewRequest): Either<ScopeContractError, Unit>

    /**
     * Updates an existing context view.
     * Returns minimal success indicator following CQRS principles.
     */
    public suspend fun updateContextView(command: UpdateContextViewRequest): Either<ScopeContractError, Unit>

    /**
     * Deletes a context view.
     * Returns minimal success indicator following CQRS principles.
     */
    public suspend fun deleteContextView(command: DeleteContextViewRequest): Either<ScopeContractError, Unit>

    /**
     * Sets a context as the current active context.
     * Returns minimal success indicator following CQRS principles.
     */
    public suspend fun setActiveContext(command: SetActiveContextRequest): Either<ScopeContractError, Unit>

    /**
     * Clears the current active context.
     * Returns minimal success indicator following CQRS principles.
     */
    public suspend fun clearActiveContext(): Either<ScopeContractError, Unit>
}
