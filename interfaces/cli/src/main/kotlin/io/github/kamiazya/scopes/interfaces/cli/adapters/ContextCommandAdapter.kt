package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.context.CreateContextViewRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.DeleteContextViewRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.SetActiveContextRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.UpdateContextViewRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError

/**
 * Command adapter for context-related CLI commands.
 * Maps between CLI commands and context view command port.
 */
class ContextCommandAdapter(private val contextViewCommandPort: ContextViewCommandPort) {
    /**
     * Create a new context view.
     */
    suspend fun createContext(request: CreateContextViewRequest): Either<ScopeContractError, Unit> = contextViewCommandPort.createContextView(request)

    /**
     * Update an existing context view.
     */
    suspend fun updateContext(request: UpdateContextViewRequest): Either<ScopeContractError, Unit> = contextViewCommandPort.updateContextView(request)

    /**
     * Delete a context view.
     */
    suspend fun deleteContext(request: DeleteContextViewRequest): Either<ScopeContractError, Unit> = contextViewCommandPort.deleteContextView(request)

    /**
     * Set a context as the current active context.
     */
    suspend fun setCurrentContext(request: SetActiveContextRequest): Either<ScopeContractError, Unit> = contextViewCommandPort.setActiveContext(request)

    /**
     * Clear the current active context.
     */
    suspend fun clearCurrentContext(): Either<ScopeContractError, Unit> = contextViewCommandPort.clearActiveContext()
}
