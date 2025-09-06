package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.context.CreateContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.context.DeleteContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.context.SetActiveContextCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.context.UpdateContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError

/**
 * Command adapter for context-related CLI commands.
 * Maps between CLI commands and context view command port.
 */
class ContextCommandAdapter(private val contextViewCommandPort: ContextViewCommandPort) {
    /**
     * Create a new context view.
     */
    suspend fun createContext(request: CreateContextViewCommand): Either<ScopeContractError, Unit> = contextViewCommandPort.createContextView(request)

    /**
     * Update an existing context view.
     */
    suspend fun updateContext(request: UpdateContextViewCommand): Either<ScopeContractError, Unit> = contextViewCommandPort.updateContextView(request)

    /**
     * Delete a context view.
     */
    suspend fun deleteContext(request: DeleteContextViewCommand): Either<ScopeContractError, Unit> = contextViewCommandPort.deleteContextView(request)

    /**
     * Set a context as the current active context.
     */
    suspend fun setCurrentContext(request: SetActiveContextCommand): Either<ScopeContractError, Unit> = contextViewCommandPort.setActiveContext(request)

    /**
     * Clear the current active context.
     */
    suspend fun clearCurrentContext(): Either<ScopeContractError, Unit> = contextViewCommandPort.clearActiveContext()
}
