package io.github.kamiazya.scopes.scopemanagement.application.command.handler.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.DeleteContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.service.ActiveContextService
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey

/**
 * Handler for deleting a context view.
 *
 * This handler validates the key, ensures the context view exists,
 * checks if it's not currently active, and deletes it from the repository.
 */
class DeleteContextViewHandler(
    private val contextViewRepository: ContextViewRepository,
    private val transactionManager: TransactionManager,
    private val activeContextService: ActiveContextService,
) : CommandHandler<DeleteContextViewCommand, ScopesError, Unit> {

    override suspend operator fun invoke(command: DeleteContextViewCommand): Either<ScopesError, Unit> = transactionManager.inTransaction {
        either {
            // Validate and create key value object
            val contextKey = ContextViewKey.create(command.key).mapLeft { it as ScopesError }.bind()

            // Check if context view exists
            val existingContext = contextViewRepository.findByKey(contextKey).mapLeft { it as ScopesError }.bind()
                ?: raise(
                    ScopesError.NotFound(
                        entityType = "ContextView",
                        identifier = command.key,
                        identifierType = "key",
                    ),
                )

            // Check if this context is currently active
            val currentContext = activeContextService.getCurrentContext()
            if (currentContext != null && currentContext.key.value == command.key) {
                raise(
                    ScopesError.ValidationFailed(
                        field = "context",
                        value = command.key,
                        constraint = ScopesError.ValidationConstraintType.InvalidValue("Cannot delete an active context"),
                    ),
                )
            }

            // Delete the context view by its ID
            contextViewRepository.deleteById(existingContext.id).mapLeft { it as ScopesError }.bind()
        }
    }
}
