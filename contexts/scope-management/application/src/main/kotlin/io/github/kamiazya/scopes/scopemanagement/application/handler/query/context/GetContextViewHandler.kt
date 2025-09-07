package io.github.kamiazya.scopes.scopemanagement.application.handler.query.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.context.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetContextView
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey
import kotlinx.datetime.Clock

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
) : QueryHandler<GetContextView, ScopesError, ContextViewDto?> {

    override suspend operator fun invoke(query: GetContextView): Either<ScopesError, ContextViewDto?> = transactionManager.inReadOnlyTransaction {
        logger.debug(
            "Getting context view by key",
            mapOf(
                "key" to query.key,
            ),
        )
        either {
            // Validate and create key value object
            val contextKey = ContextViewKey.create(query.key).bind()

            // Retrieve context view
            val contextView = contextViewRepository.findByKey(contextKey)
                .mapLeft { error ->
                    ScopesError.SystemError(
                        errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                        service = "context-repository",
                        cause = error as? Throwable,
                        context = mapOf("operation" to "find-context-view", "key" to query.key),
                        occurredAt = Clock.System.now(),
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
