package io.github.kamiazya.scopes.scopemanagement.application.handler.query.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.common.PagedResult
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.scope.GetChildren
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

/**
 * Handler for getting children of a scope.
 */
class GetChildrenHandler(private val scopeRepository: ScopeRepository, private val transactionManager: TransactionManager, private val logger: Logger) :
    QueryHandler<GetChildren, ScopesError, PagedResult<ScopeDto>> {

    override suspend operator fun invoke(query: GetChildren): Either<ScopesError, PagedResult<ScopeDto>> = transactionManager.inReadOnlyTransaction {
        logger.debug(
            "Getting children of scope",
            mapOf(
                "parentId" to (query.parentId ?: "null"),
                "offset" to query.offset,
                "limit" to query.limit,
            ),
        )

        either {
            // Parse parent ID if provided
            val parentId = query.parentId?.let { parentIdString ->
                ScopeId.create(parentIdString).bind()
            }

            // Get children from repository with database-side pagination
            val children = scopeRepository.findByParentId(parentId, query.offset, query.limit)
                .mapLeft { error ->
                    ScopesError.SystemError(
                        errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                        service = "scope-repository",
                        cause = error as? Throwable,
                        context = mapOf(
                            "operation" to "findByParentId",
                            "parentId" to (parentId?.value?.toString() ?: "null"),
                            "offset" to query.offset,
                            "limit" to query.limit,
                        ),
                        occurredAt = Clock.System.now(),
                    )
                }
                .bind()
            val totalCount = scopeRepository.countByParentId(parentId)
                .mapLeft { error ->
                    ScopesError.SystemError(
                        errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                        service = "scope-repository",
                        cause = error as? Throwable,
                        context = mapOf(
                            "operation" to "countByParentId",
                            "parentId" to (parentId?.value?.toString() ?: "null"),
                        ),
                        occurredAt = Clock.System.now(),
                    )
                }
                .bind()

            val result = PagedResult(
                items = children.map(ScopeMapper::toDto),
                offset = query.offset,
                limit = query.limit,
                totalCount = totalCount,
            )

            logger.info(
                "Successfully retrieved children of scope",
                mapOf(
                    "parentId" to (parentId?.value?.toString() ?: "null"),
                    "count" to result.items.size,
                    "totalCount" to totalCount,
                    "offset" to query.offset,
                    "limit" to query.limit,
                ),
            )

            result
        }
    }.onLeft { error ->
        logger.error(
            "Failed to get children of scope",
            mapOf(
                "parentId" to (query.parentId ?: "null"),
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                "message" to error.toString(),
            ),
        )
    }
}
