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
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasInfo
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.platform.observability.logging.Logger
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
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.ErrorMapper
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
    private val errorMapper: ErrorMapper,
    private val logger: Logger = ConsoleLogger("ScopeManagementQueryPortAdapter"),
) : ScopeManagementQueryPort {

    override suspend fun getScope(query: GetScopeQuery): Either<ScopeContractError, ScopeResult?> = getScopeByIdHandler(
        GetScopeById(
            id = query.id,
        ),
    ).fold(
        { error ->
            // For GET operations, NotFound is not an error but returns null
            if (error is ScopeError.NotFound) {
                Either.Right(null)
            } else {
                Either.Left(errorMapper.mapToContractError(error))
            }
        },
        { scopeDto ->
            Either.Right(
                scopeDto?.let {
                    ScopeResult(
                        id = it.id,
                        title = it.title,
                        description = it.description,
                        parentId = it.parentId,
                        canonicalAlias = it.canonicalAlias ?: it.id,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                        isArchived = false, // TODO: Implement archive status when available in domain
                        aspects = it.aspects,
                    )
                },
            )
        },
    )

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

    override suspend fun getScopeByAlias(query: GetScopeByAliasQuery): Either<ScopeContractError, ScopeResult> = getScopeByAliasHandler(
        AppGetScopeByAliasQuery(
            aliasName = query.aliasName,
        ),
    ).fold(
        { error ->
            Either.Left(errorMapper.mapToContractError(error))
        },
        { scopeDto ->
            scopeDto?.let {
                Either.Right(
                    ScopeResult(
                        id = it.id,
                        title = it.title,
                        description = it.description,
                        parentId = it.parentId,
                        canonicalAlias = it.canonicalAlias ?: it.id,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                        isArchived = false, // TODO: Implement archive status when available in domain
                        aspects = it.aspects,
                    ),
                )
            } ?: Either.Left(
                errorMapper.mapToContractError(
                    ScopesError.NotFound(
                        entityType = "Scope",
                        identifier = query.aliasName,
                        identifierType = "alias",
                    ),
                ),
            )
        },
    )

    override suspend fun listAliases(query: ListAliasesQuery): Either<ScopeContractError, AliasListResult> = listAliasesHandler(
        ListAliases(
            scopeId = query.scopeId,
        ),
    ).mapLeft { error ->
        errorMapper.mapToContractError(error)
    }.map { aliasDtos ->
        AliasListResult(
            scopeId = query.scopeId,
            aliases = aliasDtos.map { aliasDto ->
                AliasInfo(
                    aliasName = aliasDto.alias,
                    aliasType = if (aliasDto.isCanonical) "canonical" else "regular",
                    isCanonical = aliasDto.isCanonical,
                    createdAt = aliasDto.createdAt,
                )
            },
            totalCount = aliasDtos.size,
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
