package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.application.dto.ContextViewListResult
import io.github.kamiazya.scopes.application.dto.ContextViewResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.error.toApplicationError
import io.github.kamiazya.scopes.application.logging.Logger
import io.github.kamiazya.scopes.application.service.ActiveContextService
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.command.ListContextViewsQuery
import io.github.kamiazya.scopes.domain.repository.ContextViewRepository

/**
 * Handler for listing context views.
 *
 * This handler:
 * 1. Retrieves all context views from the repository
 * 2. Optionally includes the currently active context
 * 3. Returns a list of context view information
 */
class ListContextViewsHandler(
    private val contextViewRepository: ContextViewRepository,
    private val activeContextService: ActiveContextService,
    private val logger: Logger
) : UseCase<ListContextViewsQuery, ApplicationError, ContextViewListResult> {

    override suspend operator fun invoke(input: ListContextViewsQuery): Either<ApplicationError, ContextViewListResult> = either {
        logger.info("Listing context views", mapOf(
            "includeInactive" to input.includeInactive
        ))

        // Get all context views
        logger.debug("Fetching all context views from repository")
        val contextViews = contextViewRepository.findAll().mapLeft { error ->
            logger.error("Failed to fetch context views", mapOf(
                "error" to (error::class.simpleName ?: "Unknown")
            ))
            error.toApplicationError()
        }.bind()

        logger.debug("Found context views", mapOf("count" to contextViews.size))

        // Get the currently active context if requested
        val activeContext = if (input.includeInactive) {
            logger.debug("Fetching active context")
            activeContextService.getCurrentContext().also { context ->
                if (context != null) {
                    logger.debug("Active context found", mapOf(
                        "id" to context.id.value,
                        "name" to context.name.value
                    ))
                } else {
                    logger.debug("No active context")
                }
            }
        } else {
            logger.debug("Skipping active context check")
            null
        }

        // Map to DTOs
        val contextResults = contextViews.map { contextView ->
            ContextViewResult(
                id = contextView.id.value,
                key = contextView.key.value,
                name = contextView.name.value,
                filterExpression = contextView.filter.value,
                description = contextView.description?.value,
                isActive = activeContext?.id == contextView.id,
                createdAt = contextView.createdAt,
                updatedAt = contextView.updatedAt
            )
        }

        val activeContextResult = activeContext?.let { active ->
            contextResults.find { it.id == active.id.value }
        }

        logger.info("Context views listed successfully", mapOf(
            "total" to contextResults.size,
            "hasActive" to (activeContextResult != null)
        ))

        ContextViewListResult(
            contexts = contextResults,
            activeContext = activeContextResult
        )
    }.onLeft { error ->
        logger.error("Failed to list context views", mapOf(
            "error" to (error::class.simpleName ?: "Unknown"),
            "message" to error.toString()
        ))
    }
}

