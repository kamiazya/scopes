package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.PagedResult
import io.github.kamiazya.scopes.scopemanagement.application.dto.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.GetRootScopes
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Handler for getting root scopes (scopes without parent).
 */
class GetRootScopesHandler(private val scopeRepository: ScopeRepository, private val logger: Logger) :
    UseCase<GetRootScopes, ScopesError, PagedResult<ScopeDto>> {

    override suspend operator fun invoke(input: GetRootScopes): Either<ScopesError, PagedResult<ScopeDto>> = either {
        logger.debug(
            "Getting root scopes",
            buildMap {
                put("offset", input.offset.toString())
                put("limit", input.limit.toString())
                input.afterCreatedAt?.let { put("afterCreatedAt", it.toString()) }
                input.afterId?.let { put("afterId", it) }
            },
        )

        // Get root scopes (parentId = null) with database-side pagination
        val rootScopes = if (input.afterCreatedAt != null && input.afterId != null) {
            val cursorId = ScopeId.create(input.afterId).bind()
            scopeRepository.findByParentIdAfter(
                null,
                input.afterCreatedAt,
                cursorId,
                input.limit,
            ).bind()
        } else {
            scopeRepository.findByParentId(null, input.offset, input.limit).bind()
        }
        val totalCount = scopeRepository.countByParentId(null).bind()

        logger.debug(
            "Found root scopes",
            mapOf(
                "pageSize" to rootScopes.size.toString(),
                "totalCount" to totalCount.toString(),
            ),
        )

        // Convert to DTOs
        val result = rootScopes.map(ScopeMapper::toDto)

        logger.info(
            "Retrieved root scopes successfully",
            mapOf(
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
            "Failed to get root scopes",
            mapOf(
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                "message" to error.toString(),
            ),
        )
    }
}
