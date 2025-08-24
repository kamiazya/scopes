package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementPort
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetRootScopesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.command.CreateScope
import io.github.kamiazya.scopes.scopemanagement.application.command.DeleteScope
import io.github.kamiazya.scopes.scopemanagement.application.command.UpdateScope
import io.github.kamiazya.scopes.scopemanagement.application.handler.CreateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.DeleteScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetChildrenHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetRootScopesHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetScopeByIdHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.UpdateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.query.GetChildren
import io.github.kamiazya.scopes.scopemanagement.application.query.GetRootScopes
import io.github.kamiazya.scopes.scopemanagement.application.query.GetScopeById
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeError

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
    private val getChildrenHandler: GetChildrenHandler,
    private val getRootScopesHandler: GetRootScopesHandler,
    private val transactionManager: TransactionManager,
) : ScopeManagementPort {

    override suspend fun createScope(command: CreateScopeCommand): Either<ScopeContractError, CreateScopeResult> = transactionManager.inTransaction {
        createScopeHandler(
            CreateScope(
                title = command.title,
                description = command.description,
                parentId = command.parentId,
                generateAlias = command.generateAlias,
                customAlias = command.customAlias,
            ),
        ).mapLeft { error ->
            ErrorMapper.mapToContractError(error)
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
    }

    override suspend fun updateScope(command: UpdateScopeCommand): Either<ScopeContractError, UpdateScopeResult> = transactionManager.inTransaction {
        updateScopeHandler(
            UpdateScope(
                id = command.id,
                title = command.title,
                description = command.description,
            ),
        ).mapLeft { error ->
            ErrorMapper.mapToContractError(error)
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
    }

    override suspend fun deleteScope(command: DeleteScopeCommand): Either<ScopeContractError, Unit> = transactionManager.inTransaction {
        deleteScopeHandler(
            DeleteScope(
                id = command.id,
            ),
        ).mapLeft { error ->
            ErrorMapper.mapToContractError(error)
        }
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
                    raise(ErrorMapper.mapToContractError(error))
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
        ),
    ).mapLeft { error ->
        ErrorMapper.mapToContractError(error)
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
        GetRootScopes(),
    ).mapLeft { error ->
        ErrorMapper.mapToContractError(error)
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
}
