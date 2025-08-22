package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.GetRootScopes
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository

/**
 * Handler for getting root scopes (scopes without parent).
 */
class GetRootScopesHandler(private val scopeRepository: ScopeRepository, private val logger: Logger) : UseCase<GetRootScopes, ScopesError, List<ScopeDto>> {

    override suspend operator fun invoke(input: GetRootScopes): Either<ScopesError, List<ScopeDto>> = either {
        logger.debug(
            "Getting root scopes",
            mapOf(
                "offset" to input.offset.toString(),
                "limit" to input.limit.toString(),
            ),
        )

        // Get root scopes (parentId = null)
        val rootScopes = scopeRepository.findByParentId(null).bind()

        logger.debug(
            "Found root scopes",
            mapOf(
                "totalCount" to rootScopes.size.toString(),
            ),
        )

        // Apply pagination
        val paginatedScopes = rootScopes
            .sortedBy { it.createdAt } // Sort by creation time
            .drop(input.offset)
            .take(input.limit)

        // Convert to DTOs
        val result = paginatedScopes.map { scope ->
            ScopeMapper.toDto(scope)
        }

        logger.info(
            "Retrieved root scopes successfully",
            mapOf(
                "returnedCount" to result.size.toString(),
            ),
        )

        result
    }.onLeft { error ->
        logger.error(
            "Failed to get root scopes",
            mapOf(
                "error" to (error::class.simpleName ?: "Unknown"),
                "message" to error.toString(),
            ),
        )
    }
}
