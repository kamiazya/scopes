package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateScopeResult
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.AddAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.CreateScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.DeleteScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.RemoveAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.RenameAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.SetCanonicalAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.UpdateScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.handler.command.AddAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.command.CreateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.command.DeleteScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.command.RemoveAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.command.RenameAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.command.SetCanonicalAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.command.UpdateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetScopeById
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope.GetScopeByIdHandler
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.ErrorMapper
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.AddAliasCommand as ContractAddAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand as ContractCreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteScopeCommand as ContractDeleteScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.RemoveAliasCommand as ContractRemoveAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.RenameAliasCommand as ContractRenameAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetCanonicalAliasCommand as ContractSetCanonicalAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateScopeCommand as ContractUpdateScopeCommand

/**
 * Command port adapter implementing the ScopeManagementCommandPort interface.
 *
 * Following CQRS principles, this adapter handles only write operations (commands)
 * that modify the state of the Scope Management bounded context.
 *
 * Key responsibilities:
 * - Implement the public contract interface for commands
 * - Map between contract and application layer command types
 * - Delegate to application command handlers for business logic
 * - Use TransactionManager for command operations to ensure atomicity
 * - Map domain errors to contract errors for external consumers
 */
class ScopeManagementCommandPortAdapter(
    private val createScopeHandler: CreateScopeHandler,
    private val updateScopeHandler: UpdateScopeHandler,
    private val deleteScopeHandler: DeleteScopeHandler,
    private val getScopeByIdHandler: GetScopeByIdHandler, // Needed for alias operations
    private val addAliasHandler: AddAliasHandler,
    private val removeAliasHandler: RemoveAliasHandler,
    private val setCanonicalAliasHandler: SetCanonicalAliasHandler,
    private val renameAliasHandler: RenameAliasHandler,
    private val transactionManager: TransactionManager,
    private val logger: Logger = ConsoleLogger("ScopeManagementCommandPortAdapter"),
) : ScopeManagementCommandPort {

    override suspend fun createScope(command: ContractCreateScopeCommand): Either<ScopeContractError, CreateScopeResult> = createScopeHandler(
        CreateScopeCommand(
            title = command.title,
            description = command.description,
            parentId = command.parentId,
            generateAlias = command.generateAlias,
            customAlias = command.customAlias,
        ),
    ).mapLeft { error ->
        ErrorMapper.mapScopesErrorToScopeContractError(error)
    }.map { result ->
        CreateScopeResult(
            id = result.id,
            title = result.title,
            description = result.description,
            parentId = result.parentId,
            canonicalAlias = result.canonicalAlias ?: result.id,
            createdAt = result.createdAt,
            updatedAt = result.createdAt,
        )
    }

    override suspend fun updateScope(command: ContractUpdateScopeCommand): Either<ScopeContractError, UpdateScopeResult> = updateScopeHandler(
        UpdateScopeCommand(
            id = command.id,
            title = command.title,
            description = command.description,
        ),
    ).mapLeft { error ->
        ErrorMapper.mapScopesErrorToScopeContractError(error)
    }.map { scopeDto ->
        UpdateScopeResult(
            id = scopeDto.id,
            title = scopeDto.title,
            description = scopeDto.description,
            parentId = scopeDto.parentId,
            canonicalAlias = scopeDto.canonicalAlias ?: scopeDto.id,
            createdAt = scopeDto.createdAt,
            updatedAt = scopeDto.updatedAt,
        )
    }

    override suspend fun deleteScope(command: ContractDeleteScopeCommand): Either<ScopeContractError, Unit> = deleteScopeHandler(
        DeleteScopeCommand(
            id = command.id,
        ),
    ).mapLeft { error ->
        ErrorMapper.mapScopesErrorToScopeContractError(error)
    }

    override suspend fun addAlias(command: ContractAddAliasCommand): Either<ScopeContractError, Unit> = transactionManager.inTransaction {
        val existingAlias = getScopeByIdHandler(GetScopeById(command.scopeId)).fold(
            { error -> return@inTransaction Either.Left(ErrorMapper.mapScopesErrorToScopeContractError(error)) },
            { scope -> scope?.canonicalAlias ?: scope?.id ?: command.scopeId },
        )

        addAliasHandler(
            AddAliasCommand(
                existingAlias = existingAlias,
                newAlias = command.aliasName,
            ),
        ).mapLeft { error ->
            ErrorMapper.mapApplicationErrorToScopeContractError(error)
        }
    }

    override suspend fun removeAlias(command: ContractRemoveAliasCommand): Either<ScopeContractError, Unit> = removeAliasHandler(
        RemoveAliasCommand(
            aliasName = command.aliasName,
        ),
    ).mapLeft { error ->
        ErrorMapper.mapApplicationErrorToScopeContractError(error)
    }

    override suspend fun setCanonicalAlias(command: ContractSetCanonicalAliasCommand): Either<ScopeContractError, Unit> = transactionManager.inTransaction {
        val currentAlias = getScopeByIdHandler(GetScopeById(command.scopeId)).fold(
            { error -> return@inTransaction Either.Left(ErrorMapper.mapScopesErrorToScopeContractError(error)) },
            { scope -> scope?.canonicalAlias ?: scope?.id ?: command.scopeId },
        )

        setCanonicalAliasHandler(
            SetCanonicalAliasCommand(
                currentAlias = currentAlias,
                newCanonicalAlias = command.aliasName,
            ),
        ).mapLeft { error ->
            ErrorMapper.mapApplicationErrorToScopeContractError(error)
        }
    }

    override suspend fun renameAlias(command: ContractRenameAliasCommand): Either<ScopeContractError, Unit> = renameAliasHandler(
        RenameAliasCommand(
            currentAlias = command.oldAliasName,
            newAliasName = command.newAliasName,
        ),
    ).mapLeft { error ->
        ErrorMapper.mapApplicationErrorToScopeContractError(error)
    }
}
