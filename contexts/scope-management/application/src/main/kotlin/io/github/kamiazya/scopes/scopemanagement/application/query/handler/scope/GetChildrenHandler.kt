package io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.dto.common.PagedResult
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetChildren
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Handler for getting children of a scope.
 */
class GetChildrenHandler(private val scopeRepository: ScopeRepository, private val transactionManager: TransactionManager) :
    QueryHandler<GetChildren, ScopesError, PagedResult<ScopeDto>> {

    override suspend operator fun invoke(query: GetChildren): Either<ScopesError, PagedResult<ScopeDto>> = transactionManager.inTransaction {
        either {
            // Parse parent ID if provided
            val parentId = query.parentId?.let { parentIdString ->
                ScopeId.create(parentIdString).bind()
            }

            // Get children from repository with database-side pagination
            val children = scopeRepository.findByParentId(parentId, query.offset, query.limit)
                .mapLeft { ScopesError.SystemError("Failed to find children: $it") }
                .bind()
            val totalCount = scopeRepository.countByParentId(parentId)
                .mapLeft { ScopesError.SystemError("Failed to count children: $it") }
                .bind()

            PagedResult(
                items = children.map(ScopeMapper::toDto),
                offset = query.offset,
                limit = query.limit,
                totalCount = totalCount,
            )
        }
    }
}
