package io.github.kamiazya.scopes.contracts.scopemanagement

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetActiveContextCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateContextViewCommand
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
    public suspend fun createContextView(command: CreateContextViewCommand): Either<ScopeContractError, Unit>

    /**
     * Updates an existing context view.
     * Returns minimal success indicator following CQRS principles.
     */
    public suspend fun updateContextView(command: UpdateContextViewCommand): Either<ScopeContractError, Unit>

    /**
     * Deletes a context view.
     * Returns minimal success indicator following CQRS principles.
     */
    public suspend fun deleteContextView(command: DeleteContextViewCommand): Either<ScopeContractError, Unit>

    /**
     * Sets a context as the current active context.
     * Returns minimal success indicator following CQRS principles.
     */
    public suspend fun setActiveContext(command: SetActiveContextCommand): Either<ScopeContractError, Unit>

    /**
     * Clears the current active context.
     * Returns minimal success indicator following CQRS principles.
     */
    public suspend fun clearActiveContext(): Either<ScopeContractError, Unit>
}
