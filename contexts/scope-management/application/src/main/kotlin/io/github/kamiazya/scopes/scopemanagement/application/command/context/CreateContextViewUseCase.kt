package io.github.kamiazya.scopes.scopemanagement.application.command.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.scopemanagement.application.command.CreateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.application.error.toApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewName
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError as DomainContextError

/**
 * Use case for creating a new context view.
 *
 * This use case validates the input, ensures the key is unique,
 * validates the filter syntax, and persists the new context view.
 */
class CreateContextViewUseCase(private val contextViewRepository: ContextViewRepository, private val transactionManager: TransactionManager) {
    suspend fun execute(command: CreateContextViewCommand): Either<ApplicationError, ContextViewDto> = transactionManager.inTransaction {
        either {
            // Validate and create value objects
            val key = ContextViewKey.create(command.key)
                .mapLeft { it.toApplicationError() }
                .bind()

            val name = ContextViewName.create(command.name)
                .mapLeft { it.toApplicationError() }
                .bind()

            val filter = ContextViewFilter.create(command.filter)
                .mapLeft { it.toApplicationError() }
                .bind()

            // Description is handled by ContextView.create

            // Repository will handle duplicate key constraint

            // Create the context view
            val contextView = ContextView.create(
                key = key,
                name = name,
                filter = filter,
                description = command.description,
            ).mapLeft { (it as DomainContextError).toApplicationError() }
                .bind()

            // Save to repository
            val saved = contextViewRepository.save(contextView)
                .mapLeft { error: Any ->
                    // Map repository errors to application errors
                    PersistenceError.StorageUnavailable(
                        operation = "save-context-view",
                        cause = error.toString(),
                    )
                }
                .bind()

            // Map to DTO
            ContextViewDto(
                id = saved.id.value.toString(),
                key = saved.key.value,
                name = saved.name.value,
                filter = saved.filter.expression,
                description = saved.description?.value,
                createdAt = saved.createdAt,
                updatedAt = saved.updatedAt,
            )
        }
    }
}
