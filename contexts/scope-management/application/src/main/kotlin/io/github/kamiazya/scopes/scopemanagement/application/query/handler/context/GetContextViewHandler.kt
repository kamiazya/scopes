package io.github.kamiazya.scopes.scopemanagement.application.query.handler.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.dto.context.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetContextView
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey

/**
 * Handler for retrieving a specific context view by key.
 *
 * This handler validates the key, retrieves the context view from the repository,
 * and maps it to a DTO for external consumption.
 */
class GetContextViewHandler(private val contextViewRepository: ContextViewRepository, private val transactionManager: TransactionManager) :
    QueryHandler<GetContextView, ScopesError, ContextViewDto?> {

    override suspend operator fun invoke(query: GetContextView): Either<ScopesError, ContextViewDto?> = transactionManager.inTransaction {
        either {
            // Validate and create key value object
            val contextKey = ContextViewKey.create(query.key).bind()

            // Retrieve context view
            val contextView = contextViewRepository.findByKey(contextKey)
                .mapLeft { ScopesError.SystemError("Failed to find context view: $it") }
                .bind()

            // Map to DTO if found
            contextView?.let {
                ContextViewDto(
                    id = it.id.value.toString(),
                    key = it.key.value,
                    name = it.name.value,
                    filter = it.filter.expression,
                    description = it.description?.value,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                )
            }
        }
    }
}
