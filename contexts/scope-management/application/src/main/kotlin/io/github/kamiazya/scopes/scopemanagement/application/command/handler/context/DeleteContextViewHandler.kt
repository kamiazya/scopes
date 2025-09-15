package io.github.kamiazya.scopes.scopemanagement.application.command.handler.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.DeleteContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.error.CrossAggregateValidationError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.service.ActiveContextService
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
) : CommandHandler<DeleteContextViewCommand, ScopeManagementApplicationError, Unit> {

    override suspend operator fun invoke(command: DeleteContextViewCommand): Either<ScopeManagementApplicationError, Unit> = transactionManager.inTransaction {
        either {
            // Validate and create key value object
            val contextKey = ContextViewKey.create(command.key)
                .mapLeft { it.toGenericApplicationError() }
                .bind()

            // Check if context view exists
            val existingContext = contextViewRepository.findByKey(contextKey).fold(
                { error ->
                    raise(
                        ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                            operation = "retrieve-context-view",
                            errorCause = error.toString(),
                        ),
                    )
                },
                { context ->
                    context ?: raise(
                        ScopeManagementApplicationError.PersistenceError.NotFound(
                            entityType = "ContextView",
                            entityId = command.key,
                        ),
                    )
                },
            )

            // Check if this context is currently active
            val currentContext = activeContextService.getCurrentContext()
            if (currentContext != null && currentContext.key.value == command.key) {
                raise(
                    CrossAggregateValidationError.InvariantViolation(
                        invariantName = "active-context-deletion",
                        aggregateIds = listOf(existingContext.id.toString()),
                        violationDescription = "Cannot delete an active context",
                    ),
                )
            }

            // Delete the context view by its ID
            contextViewRepository.deleteById(existingContext.id).fold(
                { error ->
                    raise(
                        ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                            operation = "delete-context-view",
                            errorCause = error.toString(),
                        ),
                    )
                },
                { Unit },
            )
        }
    }
}
