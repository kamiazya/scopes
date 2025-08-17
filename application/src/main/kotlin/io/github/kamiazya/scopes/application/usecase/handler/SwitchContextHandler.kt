package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import io.github.kamiazya.scopes.application.dto.ContextViewInfo
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
) : CommandHandler<SwitchContext, ContextViewInfo> {

    override suspend fun invoke(command: SwitchContext): Either<ApplicationError, ContextViewInfo> {
        // Switch to the context by name
        return activeContextService.switchToContextByName(command.name).flatMap { contextView ->
            // Map to DTO
            ContextViewInfo(
                id = contextView.id.value,
                name = contextView.name.value,
                filterExpression = contextView.filter.value,
                description = contextView.description?.value,
                createdAt = contextView.createdAt.toString(),
                updatedAt = contextView.updatedAt.toString()
            ).right()
        }
    }
}