package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import arrow.core.Either
import arrow.core.either
import arrow.core.raise
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetRootScopesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAliasesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListScopesWithAspectQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListScopesWithQueryQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasInfo
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.handler.query.FilterScopesWithQueryHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.query.GetChildrenHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.query.GetRootScopesHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.query.GetScopeByAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.query.GetScopeByIdHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.query.ListAliasesHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.FilterScopesWithQuery
import io.github.kamiazya.scopes.scopemanagement.application.query.GetChildren
import io.github.kamiazya.scopes.scopemanagement.application.query.GetRootScopes
import io.github.kamiazya.scopes.scopemanagement.application.query.GetScopeById
import io.github.kamiazya.scopes.scopemanagement.application.query.ListAliases
import io.github.kamiazya.scopes.scopemanagement.domain.model.errors.ScopeError
import io.github.kamiazya.scopes.scopemanagement.infrastructure.error.ErrorMapper
import io.github.kamiazya.scopes.shared.kernel.logger.ConsoleLogger
import io.github.kamiazya.scopes.shared.kernel.logger.Logger
import io.github.kamiazya.scopes.scopemanagement.application.query.GetScopeByAliasQuery as AppGetScopeByAliasQuery

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
    private val logger: Logger = ConsoleLogger("ScopeManagementQueryPortAdapter"),
) : ScopeManagementQueryPort {

    private val errorMapper = ErrorMapper(logger.withName("ErrorMapper"))

    override suspend fun getScope(query: GetScopeQuery): Either<ScopeContractError, ScopeResult?> = either {
        getScopeByIdHandler(
            GetScopeById(
                id = query.id,
            ),
        ).fold(
            { error ->
                // For GET operations, NotFound is not an error but returns null
                if (error is ScopeError.NotFound) {
                    null
                } else {
                    raise(errorMapper.mapToContractError(error))
                }
            },
            { scopeDto ->
                ScopeResult(
                    id = scopeDto.id,
                    title = scopeDto.title,
                    description = scopeDto.description,
                    parentId = scopeDto.parentId,
                    canonicalAlias = scopeDto.canonicalAlias ?: scopeDto.id,
                    createdAt = scopeDto.createdAt,
                    updatedAt = scopeDto.updatedAt,
                    isArchived = false, // TODO: Implement archive status when available in domain
                    aspects = scopeDto.aspects,
                )
            },
        )
    }

    override suspend fun getChildren(
        query: GetChildrenQuery,
    ): Either<ScopeContractError, io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult> = getChildrenHandler(
        GetChildren(
            parentId = query.parentId,
            offset = query.offset,
            limit = query.limit,
        ),
    ).mapLeft { error ->
        errorMapper.mapToContractError(error)
    }.map { paged ->
        io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult(
            scopes = paged.items.map { scopeDto ->
                ScopeResult(
                    id = scopeDto.id,
                    title = scopeDto.title,
                    description = scopeDto.description,
                    parentId = scopeDto.parentId,
                    canonicalAlias = scopeDto.canonicalAlias ?: scopeDto.id,
                    createdAt = scopeDto.createdAt,
                    updatedAt = scopeDto.updatedAt,
                    isArchived = false, // TODO: Implement archive status when available in domain
                    aspects = scopeDto.aspects,
                )
            },
            totalCount = paged.totalCount,
            offset = paged.offset,
            limit = paged.limit,
        )
    }

    override suspend fun getRootScopes(
        query: GetRootScopesQuery,
    ): Either<ScopeContractError, io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult> = getRootScopesHandler(
        GetRootScopes(
            offset = query.offset,
            limit = query.limit,
        ),
    ).mapLeft { error ->
        errorMapper.mapToContractError(error)
    }.map { paged ->
        io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult(
            scopes = paged.items.map { scopeDto ->
                ScopeResult(
                    id = scopeDto.id,
                    title = scopeDto.title,
                    description = scopeDto.description,
                    parentId = scopeDto.parentId,
                    canonicalAlias = scopeDto.canonicalAlias ?: scopeDto.id,
                    createdAt = scopeDto.createdAt,
                    updatedAt = scopeDto.updatedAt,
                    isArchived = false, // TODO: Implement archive status when available in domain
                    aspects = scopeDto.aspects,
                )
            },
            totalCount = paged.totalCount,
            offset = paged.offset,
            limit = paged.limit,
        )
    }

    override suspend fun getScopeByAlias(query: GetScopeByAliasQuery): Either<ScopeContractError, ScopeResult> = either {
        getScopeByAliasHandler(
            AppGetScopeByAliasQuery(
                aliasName = query.aliasName,
            ),
        ).fold(
            { error ->
                raise(errorMapper.mapToContractError(error))
            },
            { scopeDto ->
                ScopeResult(
                    id = scopeDto.id,
                    title = scopeDto.title,
                    description = scopeDto.description,
                    parentId = scopeDto.parentId,
                    canonicalAlias = scopeDto.canonicalAlias ?: scopeDto.id,
                    createdAt = scopeDto.createdAt,
                    updatedAt = scopeDto.updatedAt,
                    isArchived = false, // TODO: Implement archive status when available in domain
                    aspects = scopeDto.aspects,
                )
            },
        )
    }

    override suspend fun listAliases(query: ListAliasesQuery): Either<ScopeContractError, AliasListResult> = listAliasesHandler(
        ListAliases(
            scopeId = query.scopeId,
        ),
    ).mapLeft { error ->
        errorMapper.mapToContractError(error)
    }.map { aliasListDto ->
        AliasListResult(
            scopeId = aliasListDto.scopeId,
            aliases = aliasListDto.aliases.map { aliasDto ->
                AliasInfo(
                    aliasName = aliasDto.aliasName,
                    aliasType = aliasDto.aliasType,
                    isCanonical = aliasDto.isCanonical,
                    createdAt = aliasDto.createdAt,
                )
            },
            totalCount = aliasListDto.totalCount,
        )
    }

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
    ).mapLeft { error ->
        errorMapper.mapToContractError(error)
    }.map { scopeDtos ->
        scopeDtos.map { scopeDto ->
            ScopeResult(
                id = scopeDto.id,
                title = scopeDto.title,
                description = scopeDto.description,
                parentId = scopeDto.parentId,
                canonicalAlias = scopeDto.canonicalAlias ?: scopeDto.id,
                createdAt = scopeDto.createdAt,
                updatedAt = scopeDto.updatedAt,
                isArchived = false, // TODO: Add isArchived to ScopeDto when available
                aspects = scopeDto.aspects,
            )
        }
    }
}
