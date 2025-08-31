package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.FilterScopesWithQuery
import io.github.kamiazya.scopes.scopemanagement.application.query.FilterScopesWithQueryUseCase
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError

/**
 * Handler for filtering scopes with advanced aspect queries.
 * Delegates to FilterScopesWithQueryUseCase for the actual filtering logic.
 */
class FilterScopesWithQueryHandler(private val filterScopesWithQueryUseCase: FilterScopesWithQueryUseCase, private val logger: Logger) :
    UseCase<FilterScopesWithQuery, ScopesError, List<ScopeDto>> {

    override suspend operator fun invoke(input: FilterScopesWithQuery): Either<ScopesError, List<ScopeDto>> = either {
        val contextData = mapOf(
            "query" to input.query,
            "parentId" to (input.parentId ?: "all"),
            "offset" to input.offset.toString(),
            "limit" to input.limit.toString(),
        )

        logger.debug("Filtering scopes with query", contextData)

        // Execute the use case
        val scopes = if (input.parentId != null) {
            filterScopesWithQueryUseCase.execute(input.query, input.parentId).bind()
        } else {
            filterScopesWithQueryUseCase.executeAll(input.query, input.offset, input.limit).bind()
        }

        logger.debug(
            "Query filter results",
            mapOf(
                "query" to input.query,
                "resultCount" to scopes.size.toString(),
            ),
        )

        // Convert to DTOs
        scopes.map { scope -> ScopeMapper.toDto(scope) }
    }
}
