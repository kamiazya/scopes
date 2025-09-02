package io.github.kamiazya.scopes.scopemanagement.application.command.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.application.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.application.error.toApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.service.ActiveContextService
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError as DomainContextError

/**
 * Use case for deleting a context view.
 *
 * This use case validates the key, ensures the context view exists,
 * checks if it's not currently active, and deletes it from the repository.
 */
class DeleteContextViewUseCase(
    private val contextViewRepository: ContextViewRepository,
    private val transactionManager: TransactionManager,
    private val activeContextService: ActiveContextService,
) {
    suspend fun execute(key: String): Either<ApplicationError, Unit> = transactionManager.inTransaction {
        either {
            // Validate and create key value object
            val contextKey = ContextViewKey.create(key)
                .mapLeft { it.toApplicationError() }
                .bind()

            // Check if context view exists
            val existingContext = contextViewRepository.findByKey(contextKey)
                .mapLeft { error ->
                    when (error) {
                        is DomainContextError -> error.toApplicationError()
                        else -> PersistenceError.StorageUnavailable(
                            operation = "find-context-view-by-key",
                            cause = error.toString(),
                        )
                    }
                }
                .bind()

            if (existingContext == null) {
                raise(ContextError.ContextNotFound(key = key))
            }

            // Check if this context is currently active
            val currentContext = activeContextService.getCurrentContext()
            if (currentContext != null && currentContext.key.value == key) {
                raise(ContextError.ContextInUse(key = key))
            }

            // Delete the context view by its ID
            contextViewRepository.deleteById(existingContext.id)
                .mapLeft { error ->
                    when (error) {
                        is DomainContextError -> error.toApplicationError()
                        else -> PersistenceError.StorageUnavailable(
                            operation = "delete-context-view",
                            cause = error.toString(),
                        )
                    }
                }
                .bind()
        }
    }
}
