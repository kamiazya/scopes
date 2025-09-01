package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.PagedResult
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
class GetChildrenHandler(private val scopeRepository: ScopeRepository, private val logger: Logger) : UseCase<GetChildren, ScopesError, PagedResult<ScopeDto>> {

    override suspend operator fun invoke(input: GetChildren): Either<ScopesError, PagedResult<ScopeDto>> = either {
        val contextData = buildMap {
            put("parentId", input.parentId ?: "root")
            put("offset", input.offset.toString())
            put("limit", input.limit.toString())
            input.afterCreatedAt?.let { put("afterCreatedAt", it.toString()) }
            input.afterId?.let { put("afterId", it) }
        }

        logger.debug("Getting children scopes", contextData)

        // Parse parent ID if provided
        val parentId = input.parentId?.let { parentIdString ->
            ScopeId.create(parentIdString).bind()
        }

        // Get children from repository with database-side pagination
        val children = if (input.afterCreatedAt != null && input.afterId != null) {
            val cursorId = ScopeId.create(input.afterId).bind()
            scopeRepository.findByParentIdAfter(
                parentId,
                input.afterCreatedAt,
                cursorId,
                input.limit,
            ).bind()
        } else {
            scopeRepository.findByParentId(parentId, input.offset, input.limit).bind()
        }
        val totalCount = scopeRepository.countByParentId(parentId).bind()

        logger.debug(
            "Found children",
            mapOf(
                "parentId" to (parentId?.value ?: "root"),
                "pageSize" to children.size.toString(),
                "totalCount" to totalCount.toString(),
            ),
        )

        // Convert to DTOs
        val result = children.map(ScopeMapper::toDto)

        logger.info(
            "Retrieved children successfully",
            mapOf(
                "parentId" to (parentId?.value ?: "root"),
                "returnedCount" to result.size.toString(),
            ),
        )

        PagedResult(
            items = result,
            totalCount = totalCount,
            offset = input.offset,
            limit = input.limit,
        )
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
