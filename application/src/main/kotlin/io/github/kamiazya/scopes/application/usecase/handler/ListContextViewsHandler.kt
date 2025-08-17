package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import io.github.kamiazya.scopes.application.dto.ContextViewListResult
import io.github.kamiazya.scopes.application.dto.ContextViewResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.error.DomainErrorMapper
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
    private val activeContextService: ActiveContextService
) : UseCase<ListContextViewsQuery, ApplicationError, ContextViewListResult> {

    override suspend operator fun invoke(input: ListContextViewsQuery): Either<ApplicationError, ContextViewListResult> {
        // Get all context views
        return contextViewRepository.findAll().flatMap { contextViews ->
            // Get the currently active context if requested
            val activeContext = if (input.includeInactive) {
                activeContextService.getCurrentContext()
            } else {
                null
            }

            // Map to DTOs
            val contextResults = contextViews.map { contextView ->
                ContextViewResult(
                    id = contextView.id.value,
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

            ContextViewListResult(
                contexts = contextResults,
                activeContext = activeContextResult
            ).right()
        }.mapLeft { error ->
            DomainErrorMapper.mapToApplicationError(error)
        }
    }
}
