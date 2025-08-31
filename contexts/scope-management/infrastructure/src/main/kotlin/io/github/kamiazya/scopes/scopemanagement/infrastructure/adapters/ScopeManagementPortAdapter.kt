package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementPort
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.AddAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.RemoveAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.RenameAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetCanonicalAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetRootScopesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAliasesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasInfo
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateScopeResult
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.AddAlias
import io.github.kamiazya.scopes.scopemanagement.application.command.CreateScope
import io.github.kamiazya.scopes.scopemanagement.application.command.DeleteScope
import io.github.kamiazya.scopes.scopemanagement.application.command.RemoveAlias
import io.github.kamiazya.scopes.scopemanagement.application.command.RenameAlias
import io.github.kamiazya.scopes.scopemanagement.application.command.SetCanonicalAlias
import io.github.kamiazya.scopes.scopemanagement.application.command.UpdateScope
import io.github.kamiazya.scopes.scopemanagement.application.handler.AddAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.CreateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.DeleteScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetChildrenHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetRootScopesHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetScopeByAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetScopeByIdHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.ListAliasesHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.RemoveAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.RenameAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.SetCanonicalAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.UpdateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.query.GetChildren
import io.github.kamiazya.scopes.scopemanagement.application.query.GetRootScopes
import io.github.kamiazya.scopes.scopemanagement.application.query.GetScopeById
import io.github.kamiazya.scopes.scopemanagement.application.query.ListAliases
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeError
import io.github.kamiazya.scopes.scopemanagement.application.query.GetScopeByAliasQuery as AppGetScopeByAliasQuery

/**
 * Adapter implementing the ScopeManagementPort interface.
 *
 * This adapter bridges the contract layer with the application layer,
 * translating between contract commands/queries and application-specific
 * commands/queries while managing transaction boundaries and error mapping.
 *
 * Key responsibilities:
 * - Implement the public contract interface
 * - Map between contract and application layer types
 * - Delegate to application handlers for business logic
 * - Use TransactionManager for command operations
 * - Map domain errors to contract errors
 */
class ScopeManagementPortAdapter(
    private val createScopeHandler: CreateScopeHandler,
    private val updateScopeHandler: UpdateScopeHandler,
    private val deleteScopeHandler: DeleteScopeHandler,
    private val getScopeByIdHandler: GetScopeByIdHandler,
    private val getScopeByAliasHandler: GetScopeByAliasHandler,
    private val getChildrenHandler: GetChildrenHandler,
    private val getRootScopesHandler: GetRootScopesHandler,
    private val listAliasesHandler: ListAliasesHandler,
    private val addAliasHandler: AddAliasHandler,
    private val removeAliasHandler: RemoveAliasHandler,
    private val setCanonicalAliasHandler: SetCanonicalAliasHandler,
    private val renameAliasHandler: RenameAliasHandler,
    private val transactionManager: TransactionManager,
    private val logger: Logger = ConsoleLogger("ScopeManagementPortAdapter"),
) : ScopeManagementPort {

    private val errorMapper = ErrorMapper(logger.withName("ErrorMapper"))

    override suspend fun createScope(command: CreateScopeCommand): Either<ScopeContractError, CreateScopeResult> = createScopeHandler(
        CreateScope(
            title = command.title,
            description = command.description,
            parentId = command.parentId,
            generateAlias = command.generateAlias,
            customAlias = command.customAlias,
        ),
    ).mapLeft { error ->
        errorMapper.mapToContractError(error)
    }.map { result ->
        CreateScopeResult(
            id = result.id,
            title = result.title,
            description = result.description,
            parentId = result.parentId,
            canonicalAlias = result.canonicalAlias ?: "@${result.id}",
            createdAt = result.createdAt,
            updatedAt = result.createdAt,
        )
    }

    override suspend fun updateScope(command: UpdateScopeCommand): Either<ScopeContractError, UpdateScopeResult> = updateScopeHandler(
        UpdateScope(
            id = command.id,
            title = command.title,
            description = command.description,
        ),
    ).mapLeft { error ->
        errorMapper.mapToContractError(error)
    }.map { scopeDto ->
        UpdateScopeResult(
            id = scopeDto.id,
            title = scopeDto.title,
            description = scopeDto.description,
            parentId = scopeDto.parentId,
            canonicalAlias = scopeDto.canonicalAlias ?: "@${scopeDto.id}",
            createdAt = scopeDto.createdAt,
            updatedAt = scopeDto.updatedAt,
        )
    }

    override suspend fun deleteScope(command: DeleteScopeCommand): Either<ScopeContractError, Unit> = deleteScopeHandler(
        DeleteScope(
            id = command.id,
        ),
    ).mapLeft { error ->
        errorMapper.mapToContractError(error)
    }

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
                    canonicalAlias = scopeDto.canonicalAlias ?: "@${scopeDto.id}",
                    createdAt = scopeDto.createdAt,
                    updatedAt = scopeDto.updatedAt,
                    isArchived = false, // TODO: Implement archive status when available in domain
                    aspects = scopeDto.aspects,
                )
            },
        )
    }

    override suspend fun getChildren(query: GetChildrenQuery): Either<ScopeContractError, List<ScopeResult>> = getChildrenHandler(
        GetChildren(
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
                canonicalAlias = scopeDto.canonicalAlias ?: "@${scopeDto.id}",
                createdAt = scopeDto.createdAt,
                updatedAt = scopeDto.updatedAt,
                isArchived = false, // TODO: Implement archive status when available in domain
                aspects = scopeDto.aspects,
            )
        }
    }

    override suspend fun getRootScopes(query: GetRootScopesQuery): Either<ScopeContractError, List<ScopeResult>> = getRootScopesHandler(
        GetRootScopes(
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
                canonicalAlias = scopeDto.canonicalAlias ?: "@${scopeDto.id}",
                createdAt = scopeDto.createdAt,
                updatedAt = scopeDto.updatedAt,
                isArchived = false, // TODO: Implement archive status when available in domain
                aspects = scopeDto.aspects,
            )
        }
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
                    canonicalAlias = scopeDto.canonicalAlias ?: "@${scopeDto.id}",
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

    override suspend fun addAlias(command: AddAliasCommand): Either<ScopeContractError, Unit> = either {
        val existingAlias = getScopeByIdHandler(GetScopeById(command.scopeId)).fold(
            { error -> raise(errorMapper.mapToContractError(error)) },
            { scope -> scope.canonicalAlias ?: "@${scope.id}" },
        )

        addAliasHandler(
            AddAlias(
                existingAlias = existingAlias,
                newAlias = command.aliasName,
            ),
        ).fold(
            { error -> raise(errorMapper.mapToContractError(error)) },
            { Unit },
        )
    }

    override suspend fun removeAlias(command: RemoveAliasCommand): Either<ScopeContractError, Unit> = removeAliasHandler(
        RemoveAlias(
            aliasName = command.aliasName,
        ),
    ).mapLeft { error ->
        errorMapper.mapToContractError(error)
    }

    override suspend fun setCanonicalAlias(command: SetCanonicalAliasCommand): Either<ScopeContractError, Unit> = either {
        val currentAlias = getScopeByIdHandler(GetScopeById(command.scopeId)).fold(
            { error -> raise(errorMapper.mapToContractError(error)) },
            { scope -> scope.canonicalAlias ?: "@${scope.id}" },
        )

        setCanonicalAliasHandler(
            SetCanonicalAlias(
                currentAlias = currentAlias,
                newCanonicalAlias = command.aliasName,
            ),
        ).fold(
            { error -> raise(errorMapper.mapToContractError(error)) },
            { Unit },
        )
    }

    override suspend fun renameAlias(command: RenameAliasCommand): Either<ScopeContractError, Unit> = renameAliasHandler(
        RenameAlias(
            currentAlias = command.oldAliasName,
            newAliasName = command.newAliasName,
        ),
    ).mapLeft { error ->
        errorMapper.mapToContractError(error)
    }
}
