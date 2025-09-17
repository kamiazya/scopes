package io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.common.PagedResult
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetRootScopes
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository

/**
 * Handler for getting root scopes (scopes without parent).
 */
class GetRootScopesHandler(private val scopeRepository: ScopeRepository, private val transactionManager: TransactionManager, private val logger: Logger) :
    QueryHandler<GetRootScopes, ScopeManagementApplicationError, PagedResult<ScopeDto>> {

    override suspend operator fun invoke(query: GetRootScopes): Either<ScopeManagementApplicationError, PagedResult<ScopeDto>> =
        transactionManager.inReadOnlyTransaction {
            logger.debug(
                "Getting root scopes",
                mapOf(
                    "offset" to query.offset,
                    "limit" to query.limit,
                ),
            )
            either {
                // Get root scopes (parentId = null) with database-side pagination
                val rootScopes = scopeRepository.findByParentId(null, query.offset, query.limit)
                    .mapLeft { _ ->
                        ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                            operation = "find-root-scopes",
                        )
                    }
                    .bind()
                val totalCount = scopeRepository.countByParentId(null)
                    .mapLeft { _ ->
                        ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                            operation = "count-root-scopes",
                        )
                    }
                    .bind()

                val result = PagedResult(
                    items = rootScopes.map(ScopeMapper::toDto),
                    offset = query.offset,
                    limit = query.limit,
                    totalCount = totalCount,
                )

                logger.info(
                    "Successfully retrieved root scopes",
                    mapOf(
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
                "Failed to get root scopes",
                mapOf(
                    "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                    "message" to error.toString(),
                ),
            )
        }
}
