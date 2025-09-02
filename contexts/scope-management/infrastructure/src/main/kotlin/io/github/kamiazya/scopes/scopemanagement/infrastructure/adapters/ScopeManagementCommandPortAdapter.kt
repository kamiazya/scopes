package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.AddAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.RemoveAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.RenameAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetCanonicalAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateScopeResult
import io.github.kamiazya.scopes.platform.kernel.logger.ConsoleLogger
import io.github.kamiazya.scopes.platform.kernel.logger.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.AddAlias
import io.github.kamiazya.scopes.scopemanagement.application.command.CreateScope
import io.github.kamiazya.scopes.scopemanagement.application.command.DeleteScope
import io.github.kamiazya.scopes.scopemanagement.application.command.RemoveAlias
import io.github.kamiazya.scopes.scopemanagement.application.command.RenameAlias
import io.github.kamiazya.scopes.scopemanagement.application.command.SetCanonicalAlias
import io.github.kamiazya.scopes.scopemanagement.application.command.UpdateScope
import io.github.kamiazya.scopes.scopemanagement.application.handler.command.AddAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.command.CreateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.command.DeleteScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.command.RemoveAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.command.RenameAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.command.SetCanonicalAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.command.UpdateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.query.GetScopeByIdHandler
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.query.GetScopeById
import io.github.kamiazya.scopes.scopemanagement.infrastructure.error.ErrorMapper

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

    private val errorMapper = ErrorMapper(ConsoleLogger("${logger.name}.ErrorMapper"))

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
            canonicalAlias = result.canonicalAlias ?: result.id,
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
            canonicalAlias = scopeDto.canonicalAlias ?: scopeDto.id,
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

    override suspend fun addAlias(command: AddAliasCommand): Either<ScopeContractError, Unit> = transactionManager.inTransaction {
        val existingAlias = getScopeByIdHandler(GetScopeById(command.scopeId)).fold(
            { error -> return@inTransaction Either.Left(errorMapper.mapToContractError(error)) },
            { scope -> scope.canonicalAlias ?: scope.id },
        )

        addAliasHandler(
            AddAlias(
                existingAlias = existingAlias,
                newAlias = command.aliasName,
            ),
        ).mapLeft { error ->
            errorMapper.mapToContractError(error)
        }
    }

    override suspend fun removeAlias(command: RemoveAliasCommand): Either<ScopeContractError, Unit> = removeAliasHandler(
        RemoveAlias(
            aliasName = command.aliasName,
        ),
    ).mapLeft { error ->
        errorMapper.mapToContractError(error)
    }

    override suspend fun setCanonicalAlias(command: SetCanonicalAliasCommand): Either<ScopeContractError, Unit> = transactionManager.inTransaction {
        val currentAlias = getScopeByIdHandler(GetScopeById(command.scopeId)).fold(
            { error -> return@inTransaction Either.Left(errorMapper.mapToContractError(error)) },
            { scope -> scope.canonicalAlias ?: scope.id },
        )

        setCanonicalAliasHandler(
            SetCanonicalAlias(
                currentAlias = currentAlias,
                newCanonicalAlias = command.aliasName,
            ),
        ).mapLeft { error ->
            errorMapper.mapToContractError(error)
        }
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
