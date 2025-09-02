package io.github.kamiazya.scopes.scopemanagement.application.query.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.scopemanagement.application.dto.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.application.error.toApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError as DomainContextError

/**
 * Use case for listing all context views.
 *
 * This use case retrieves all context views from the repository
 * and maps them to DTOs for external consumption.
 */
class ListContextViewsUseCase(private val contextViewRepository: ContextViewRepository, private val transactionManager: TransactionManager) {
    suspend fun execute(): Either<ApplicationError, List<ContextViewDto>> = transactionManager.inTransaction {
        either {
            val contextViews = contextViewRepository.findAll()
                .mapLeft { error ->
                    when (error) {
                        is DomainContextError -> error.toApplicationError()
                        else -> PersistenceError.StorageUnavailable(
                            operation = "find-all-context-views",
                            cause = error.toString(),
                        )
                    }
                }
                .bind()

            contextViews.map { contextView ->
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
}
