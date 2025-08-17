package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import io.github.kamiazya.scopes.application.dto.ContextViewResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.service.ActiveContextService
import io.github.kamiazya.scopes.application.usecase.command.SwitchContext

/**
 * Handler for switching the active context view.
 * 
 * This handler:
 * 1. Validates the context name
 * 2. Switches to the specified context
 * 3. Returns the now-active context information
 */
class SwitchContextHandler(
    private val activeContextService: ActiveContextService
) : CommandHandler<SwitchContext, ContextViewResult> {

    override suspend fun invoke(command: SwitchContext): Either<ApplicationError, ContextViewResult> {
        // Switch to the context by name
        return activeContextService.switchToContextByName(command.name).flatMap { contextView ->
            // Map to DTO
            ContextViewResult(
                id = contextView.id.value,
                name = contextView.name.value,
                filterExpression = contextView.filter.value,
                description = contextView.description?.value,
                isActive = true, // This context is now active
                createdAt = contextView.createdAt,
                updatedAt = contextView.updatedAt
            ).right()
        }
    }
}