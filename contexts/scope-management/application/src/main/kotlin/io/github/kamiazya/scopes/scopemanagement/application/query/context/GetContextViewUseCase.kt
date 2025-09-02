package io.github.kamiazya.scopes.scopemanagement.application.query.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.scopemanagement.application.dto.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.application.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.application.error.toApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError as DomainContextError

/**
 * Use case for retrieving a specific context view by key.
 *
 * This use case validates the key, retrieves the context view from the repository,
 * and maps it to a DTO for external consumption.
 */
class GetContextViewUseCase(private val contextViewRepository: ContextViewRepository, private val transactionManager: TransactionManager) {
    suspend fun execute(key: String): Either<ApplicationError, ContextViewDto> = transactionManager.inTransaction {
        either {
            // Validate and create key value object
            val contextKey = ContextViewKey.create(key)
                .mapLeft { it.toApplicationError() }
                .bind()

            // Retrieve context view
            val contextView = contextViewRepository.findByKey(contextKey)
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

            if (contextView == null) {
                raise(ContextError.ContextNotFound(key = key))
            }

            // Map to DTO
            ContextViewDto(
                id = contextView.id.value.toString(),
                key = contextView.key.value,
                name = contextView.name.value,
                filter = contextView.filter.expression,
                description = contextView.description?.value,
                createdAt = contextView.createdAt,
                updatedAt = contextView.updatedAt,
            )
        }
    }
}
