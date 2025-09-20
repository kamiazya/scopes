package io.github.kamiazya.scopes.scopemanagement.application.command.handler.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.DeleteContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.service.ActiveContextService
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey

/**
 * Handler for deleting a context view.
 *
 * This handler validates the key, ensures the context view exists,
 * checks if it's not currently active, and deletes it from the repository.
 * Uses BaseCommandHandler for common functionality like transaction management and logging.
 */
class DeleteContextViewHandler(
    private val contextViewRepository: ContextViewRepository,
    private val activeContextService: ActiveContextService,
    private val applicationErrorMapper: ApplicationErrorMapper,
    transactionManager: TransactionManager,
    logger: Logger,
) : BaseCommandHandler<DeleteContextViewCommand, Unit>(transactionManager, logger) {

    override suspend fun executeCommand(command: DeleteContextViewCommand): Either<ScopeContractError, Unit> = either {
        // Validate and create key value object
        val contextKey = ContextViewKey.create(command.key)
            .mapLeft { applicationErrorMapper.mapDomainError(it) }
            .bind()

        // Check if context view exists
        val existingContext = contextViewRepository.findByKey(contextKey).fold(
            { _ ->
                raise(
                    ScopeContractError.SystemError.ServiceUnavailable(
                        service = "context-view-repository",
                    ),
                )
            },
            { context ->
                context ?: raise(
                    ScopeContractError.BusinessError.ContextNotFound(
                        contextKey = command.key,
                    ),
                )
            },
        )

        // Check if this context is currently active
        val currentContext = activeContextService.getCurrentContext()
        if (currentContext != null && currentContext.key.value == command.key) {
            raise(
                ScopeContractError.BusinessError.DuplicateContextKey(
                    contextKey = command.key,
                    existingContextId = "active", // Indicates the context is currently active
                ),
            )
        }

        // Delete the context view by its ID
        contextViewRepository.deleteById(existingContext.id).fold(
            { _ ->
                raise(
                    ScopeContractError.SystemError.ServiceUnavailable(
                        service = "context-view-repository",
                    ),
                )
            },
            { Unit },
        )
    }
}
