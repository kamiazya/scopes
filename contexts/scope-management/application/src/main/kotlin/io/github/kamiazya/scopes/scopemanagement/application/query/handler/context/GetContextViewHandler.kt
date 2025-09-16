package io.github.kamiazya.scopes.scopemanagement.application.query.handler.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.context.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetContextView
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey

/**
 * Handler for retrieving a specific context view by key.
 *
 * This handler validates the key, retrieves the context view from the repository,
 * and maps it to a DTO for external consumption.
 */
class GetContextViewHandler(
    private val contextViewRepository: ContextViewRepository,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
) : QueryHandler<GetContextView, ScopeManagementApplicationError, ContextViewDto?> {

    override suspend operator fun invoke(query: GetContextView): Either<ScopeManagementApplicationError, ContextViewDto?> =
        transactionManager.inReadOnlyTransaction {
            logger.debug(
                "Getting context view by key",
                mapOf(
                    "key" to query.key,
                ),
            )
            either {
                // Validate and create key value object
                val contextKey = ContextViewKey.create(query.key)
                    .mapLeft { it.toGenericApplicationError() }
                    .bind()

                // Retrieve context view
                val contextView = contextViewRepository.findByKey(contextKey)
                    .mapLeft { _ ->
                        ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                            operation = "find-context-view",
                        )
                    }
                    .bind()

                // Map to DTO if found
                val result = contextView?.let {
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

                logger.info(
                    "Context view lookup completed",
                    mapOf(
                        "key" to contextKey.value,
                        "found" to (result != null).toString(),
                    ),
                )

                result
            }
        }.onLeft { error ->
            logger.error(
                "Failed to get context view",
                mapOf(
                    "key" to query.key,
                    "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                    "message" to error.toString(),
                ),
            )
        }
}
