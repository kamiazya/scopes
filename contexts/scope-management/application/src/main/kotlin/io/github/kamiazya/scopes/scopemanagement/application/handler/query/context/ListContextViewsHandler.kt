package io.github.kamiazya.scopes.scopemanagement.application.handler.query.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.context.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.ListContextViews
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import kotlinx.datetime.Clock

/**
 * Handler for listing all context views.
 *
 * This handler retrieves all context views from the repository
 * and maps them to DTOs for external consumption.
 */
class ListContextViewsHandler(
    private val contextViewRepository: ContextViewRepository,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
) : QueryHandler<ListContextViews, ScopesError, List<ContextViewDto>> {

    override suspend operator fun invoke(query: ListContextViews): Either<ScopesError, List<ContextViewDto>> = transactionManager.inReadOnlyTransaction {
        logger.debug(
            "Listing all context views",
            mapOf<String, Any>(),
        )
        either {
            // Retrieve all context views
            val contextViews = contextViewRepository.findAll()
                .mapLeft { error ->
                    ScopesError.SystemError(
                        errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                        service = "context-repository",
                        cause = error as? Throwable,
                        context = mapOf("operation" to "findAll"),
                        occurredAt = Clock.System.now(),
                    )
                }
                .bind()

            // Map to DTOs
            val result = contextViews.map { contextView ->
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

            logger.info(
                "Successfully listed context views",
                mapOf(
                    "count" to result.size,
                ),
            )

            result
        }
    }.onLeft { error ->
        logger.error(
            "Failed to list context views",
            mapOf(
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                "message" to error.toString(),
            ),
        )
    }
}
