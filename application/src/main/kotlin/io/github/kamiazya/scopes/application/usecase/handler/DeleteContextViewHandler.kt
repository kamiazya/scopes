package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.application.dto.EmptyResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.error.DomainErrorMapper
import io.github.kamiazya.scopes.application.port.Logger
import io.github.kamiazya.scopes.application.service.ActiveContextService
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.command.DeleteContextView
import io.github.kamiazya.scopes.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.domain.valueobject.ContextViewId

/**
 * Handler for deleting a context view.
 *
 * This handler:
 * 1. Validates the context ID
 * 2. Checks if the context is currently active (cannot delete active context)
 * 3. Deletes the context view from the repository
 * 4. Returns an empty result on success
 */
class DeleteContextViewHandler(
    private val contextViewRepository: ContextViewRepository,
    private val activeContextService: ActiveContextService,
    private val logger: Logger
) : UseCase<DeleteContextView, ApplicationError, EmptyResult> {

    override suspend operator fun invoke(input: DeleteContextView): Either<ApplicationError, EmptyResult> = either {
        logger.info("Deleting context view", mapOf("id" to input.id))
        
        // Parse the context ID
        logger.debug("Parsing context ID", mapOf("id" to input.id))
        val contextId = ContextViewId.create(input.id).mapLeft { error ->
            logger.warn("Invalid context ID format", mapOf(
                "id" to input.id,
                "error" to (error::class.simpleName ?: "Unknown")
            ))
            DomainErrorMapper.mapToApplicationError(error)
        }.bind()

        // Check if this context is currently active
        logger.debug("Checking if context is active", mapOf("id" to contextId.value))
        val activeContext = activeContextService.getCurrentContext()
        
        ensure(activeContext?.id != contextId) {
            logger.error("Cannot delete active context", mapOf(
                "id" to contextId.value,
                "activeContextId" to (activeContext?.id?.value ?: "null")
            ))
            ApplicationError.ContextError.ActiveContextDeleteAttempt(
                contextId.value
            )
        }
        
        // Delete the context view
        logger.debug("Deleting context from repository", mapOf("id" to contextId.value))
        contextViewRepository.delete(contextId).mapLeft { error ->
            logger.error("Failed to delete context view", mapOf(
                "id" to contextId.value,
                "error" to (error::class.simpleName ?: "Unknown")
            ))
            DomainErrorMapper.mapToApplicationError(error)
        }.bind()
        
        logger.info("Context view deleted successfully", mapOf("id" to contextId.value))
        EmptyResult
    }.onLeft { error ->
        logger.error("Failed to delete context view", mapOf(
            "error" to (error::class.simpleName ?: "Unknown"),
            "message" to error.toString()
        ))
    }
}
