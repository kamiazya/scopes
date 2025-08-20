package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.application.dto.ContextViewResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.logging.Logger
import io.github.kamiazya.scopes.application.service.ActiveContextService
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.command.SwitchContextView

/**
 * Handler for switching the active context view.
 *
 * This handler:
 * 1. Validates the context key
 * 2. Switches to the specified context
 * 3. Returns the now-active context information
 */
class SwitchContextHandler(private val activeContextService: ActiveContextService, private val logger: Logger) : UseCase<SwitchContextView, ApplicationError, ContextViewResult> {

    override suspend operator fun invoke(input: SwitchContextView): Either<ApplicationError, ContextViewResult> = either {
        logger.info("Switching context", mapOf("contextKey" to input.key))

        // Switch to the context by key
        val contextView = activeContextService.switchToContextByKey(input.key)
            .onLeft { error ->
                logger.error(
                    "Failed to switch context",
                    mapOf(
                        "contextKey" to input.key,
                        "error" to (error::class.simpleName ?: "Unknown"),
                    ),
                )
            }
            .bind()

        logger.info(
            "Context switched successfully",
            mapOf(
                "contextId" to contextView.id.value,
                "contextKey" to contextView.key.value,
                "contextName" to contextView.name.value,
            ),
        )

        // Map to DTO
        ContextViewResult(
            id = contextView.id.value,
            key = contextView.key.value,
            name = contextView.name.value,
            filterExpression = contextView.filter.value,
            description = contextView.description?.value,
            isActive = true, // This context is now active
            createdAt = contextView.createdAt,
            updatedAt = contextView.updatedAt,
        )
    }.onLeft { error ->
        logger.error(
            "Context switch failed",
            mapOf(
                "error" to (error::class.simpleName ?: "Unknown"),
                "message" to error.toString(),
            ),
        )
    }
}
