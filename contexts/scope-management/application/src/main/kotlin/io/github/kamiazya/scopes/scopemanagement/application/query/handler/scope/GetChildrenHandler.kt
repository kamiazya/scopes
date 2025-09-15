package io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.common.PagedResult
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetChildren
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Handler for getting children of a scope.
 */
class GetChildrenHandler(private val scopeRepository: ScopeRepository, private val transactionManager: TransactionManager, private val logger: Logger) :
    QueryHandler<GetChildren, ScopeManagementApplicationError, PagedResult<ScopeDto>> {

    override suspend operator fun invoke(query: GetChildren): Either<ScopeManagementApplicationError, PagedResult<ScopeDto>> =
        transactionManager.inReadOnlyTransaction {
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
                    ScopeId.create(parentIdString)
                        .mapLeft { it.toGenericApplicationError() }
                        .bind()
                }

                // Get children from repository with database-side pagination
                val children = scopeRepository.findByParentId(parentId, query.offset, query.limit)
                    .mapLeft { error ->
                        ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                            operation = "findByParentId",
                            errorCause = error.toString(),
                        )
                    }
                    .bind()
                val totalCount = scopeRepository.countByParentId(parentId)
                    .mapLeft { error ->
                        ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                            operation = "countByParentId",
                            errorCause = error.toString(),
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
