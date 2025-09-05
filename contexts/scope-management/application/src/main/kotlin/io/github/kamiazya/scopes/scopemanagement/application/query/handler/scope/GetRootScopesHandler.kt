package io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.dto.common.PagedResult
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetRootScopes
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository

/**
 * Handler for getting root scopes (scopes without parent).
 */
class GetRootScopesHandler(private val scopeRepository: ScopeRepository, private val transactionManager: TransactionManager) :
    QueryHandler<GetRootScopes, ScopesError, PagedResult<ScopeDto>> {

    override suspend operator fun invoke(query: GetRootScopes): Either<ScopesError, PagedResult<ScopeDto>> = transactionManager.inTransaction {
        either {
            // Get root scopes (parentId = null) with database-side pagination
            val rootScopes = scopeRepository.findByParentId(null, query.offset, query.limit)
                .mapLeft { error ->
                    ScopesError.SystemError(
                        errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                        service = "scope-repository",
                        cause = error as? Throwable,
                        context = mapOf(
                            "operation" to "findByParentId",
                            "parentId" to "null",
                            "offset" to query.offset,
                            "limit" to query.limit,
                        ),
                    )
                }
                .bind()
            val totalCount = scopeRepository.countByParentId(null)
                .mapLeft { error ->
                    ScopesError.SystemError(
                        errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                        service = "scope-repository",
                        cause = error as? Throwable,
                        context = mapOf(
                            "operation" to "countByParentId",
                            "parentId" to "null",
                        ),
                    )
                }
                .bind()

            PagedResult(
                items = rootScopes.map(ScopeMapper::toDto),
                offset = query.offset,
                limit = query.limit,
                totalCount = totalCount,
            )
        }
    }
}
