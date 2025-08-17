package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.application.dto.EmptyResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.error.DomainErrorMapper
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
    private val activeContextService: ActiveContextService
) : UseCase<DeleteContextView, ApplicationError, EmptyResult> {

    override suspend operator fun invoke(input: DeleteContextView): Either<ApplicationError, EmptyResult> {
        // Parse the context ID
        val contextIdResult = ContextViewId.create(input.id).mapLeft { error ->
            DomainErrorMapper.mapToApplicationError(error)
        }

        return contextIdResult.flatMap { contextId ->
            // Check if this context is currently active
            val activeContext = activeContextService.getCurrentContext()
            if (activeContext != null && activeContext.id == contextId) {
                ApplicationError.ContextError.ActiveContextDeleteAttempt(
                    contextId.value
                ).left()
            } else {
                // Delete the context view
                contextViewRepository.delete(contextId).flatMap {
                    EmptyResult.right()
                }.mapLeft { error ->
                    DomainErrorMapper.mapToApplicationError(error)
                }
            }
        }
    }
}
