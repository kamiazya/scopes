package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetRootScopesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAliasesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListScopesWithAspectQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListScopesWithQueryQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.FilterScopesWithQuery
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetChildren
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetRootScopes
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetScopeById
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.ListAliases
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope.FilterScopesWithQueryHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope.GetChildrenHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope.GetRootScopesHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope.GetScopeByAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope.GetScopeByIdHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope.ListAliasesHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetScopeByAlias as AppGetScopeByAliasQuery

/**
 * Query port adapter implementing the ScopeManagementQueryPort interface.
 *
 * Following CQRS principles, this adapter handles only read operations (queries)
 * that retrieve data from the Scope Management bounded context without side effects.
 *
 * Key responsibilities:
 * - Implement the public contract interface for queries
 * - Map between contract and application layer query types
 * - Delegate to application query handlers for data retrieval
 * - Optimize read operations for performance (no transaction overhead)
 * - Map domain errors to contract errors for external consumers
 * - Support read-only projections and view models (future enhancement)
 */
class ScopeManagementQueryPortAdapter(
    private val getScopeByIdHandler: GetScopeByIdHandler,
    private val getScopeByAliasHandler: GetScopeByAliasHandler,
    private val getChildrenHandler: GetChildrenHandler,
    private val getRootScopesHandler: GetRootScopesHandler,
    private val listAliasesHandler: ListAliasesHandler,
    private val filterScopesWithQueryHandler: FilterScopesWithQueryHandler,
    private val applicationErrorMapper: ApplicationErrorMapper,
    private val logger: Logger = ConsoleLogger("ScopeManagementQueryPortAdapter"),
) : ScopeManagementQueryPort {

    override suspend fun getScope(query: GetScopeQuery): Either<ScopeContractError, ScopeResult?> = getScopeByIdHandler(
        GetScopeById(
            id = query.id,
        ),
    )

    override suspend fun getChildren(
        query: GetChildrenQuery,
    ): Either<ScopeContractError, io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult> = getChildrenHandler(
        GetChildren(
            parentId = query.parentId,
            offset = query.offset,
            limit = query.limit,
        ),
    )

    override suspend fun getRootScopes(
        query: GetRootScopesQuery,
    ): Either<ScopeContractError, io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult> = getRootScopesHandler(
        GetRootScopes(
            offset = query.offset,
            limit = query.limit,
        ),
    )

    override suspend fun getScopeByAlias(query: GetScopeByAliasQuery): Either<ScopeContractError, ScopeResult> = getScopeByAliasHandler(
        AppGetScopeByAliasQuery(
            aliasName = query.aliasName,
        ),
    ).fold(
        { error ->
            Either.Left(error)
        },
        { scopeResult ->
            scopeResult?.let {
                Either.Right(it)
            } ?: Either.Left(
                ScopeContractError.BusinessError.AliasNotFound(
                    alias = query.aliasName,
                ),
            )
        },
    )

    override suspend fun listAliases(query: ListAliasesQuery): Either<ScopeContractError, AliasListResult> = listAliasesHandler(
        ListAliases(
            scopeId = query.scopeId,
        ),
    )

    override suspend fun listScopesWithAspect(query: ListScopesWithAspectQuery): Either<ScopeContractError, List<ScopeResult>> {
        // Construct a query string and delegate to the advanced query handler
        val aspectQuery = "\"${query.aspectKey}\"=\"${query.aspectValue}\""
        val listQuery = ListScopesWithQueryQuery(
            aspectQuery = aspectQuery,
            parentId = query.parentId,
            offset = query.offset,
            limit = query.limit,
        )
        return listScopesWithQuery(listQuery)
    }

    override suspend fun listScopesWithQuery(query: ListScopesWithQueryQuery): Either<ScopeContractError, List<ScopeResult>> = filterScopesWithQueryHandler(
        FilterScopesWithQuery(
            query = query.aspectQuery,
            parentId = query.parentId,
            offset = query.offset,
            limit = query.limit,
        ),
    )
}
