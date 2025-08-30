package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.GetChildren
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Handler for getting children of a scope.
 */
class GetChildrenHandler(private val scopeRepository: ScopeRepository, private val logger: Logger) : UseCase<GetChildren, ScopesError, List<ScopeDto>> {

    override suspend operator fun invoke(input: GetChildren): Either<ScopesError, List<ScopeDto>> = either {
        val contextData = mapOf(
            "parentId" to (input.parentId ?: "root"),
            "offset" to input.offset.toString(),
            "limit" to input.limit.toString(),
        )

        logger.debug("Getting children scopes", contextData)

        // Parse parent ID if provided
        val parentId = input.parentId?.let { parentIdString ->
            ScopeId.create(parentIdString).bind()
        }

        // Get children from repository
        val children = scopeRepository.findByParentId(parentId).bind()

        logger.debug(
            "Found children",
            mapOf(
                "parentId" to (parentId?.value ?: "root"),
                "totalCount" to children.size.toString(),
            ),
        )

        // Apply pagination
        val paginatedChildren = children
            .sortedBy { it.createdAt } // Sort by creation time
            .drop(input.offset)
            .take(input.limit)

        // Convert to DTOs
        val result = paginatedChildren.map { scope ->
            ScopeMapper.toDto(scope)
        }

        logger.info(
            "Retrieved children successfully",
            mapOf(
                "parentId" to (parentId?.value ?: "root"),
                "returnedCount" to result.size.toString(),
            ),
        )

        result
    }.onLeft { error ->
        logger.error(
            "Failed to get children",
            mapOf(
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                "message" to error.toString(),
            ),
        )
    }
}
