package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateScopeResult
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.AddAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.DeleteScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.RemoveAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.RenameAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.SetCanonicalAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.UpdateScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.AddAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.CreateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.DeleteScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.RemoveAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.RenameAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.SetCanonicalAliasHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.UpdateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetScopeById
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope.GetScopeByIdHandler
import org.jmolecules.architecture.hexagonal.SecondaryAdapter
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
 *
 * Marked with @SecondaryAdapter to indicate this is an implementation of a driving port
 * in hexagonal architecture, adapting between external contracts and internal application logic.
 */
@SecondaryAdapter
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
    private val applicationErrorMapper: ApplicationErrorMapper,
) : ScopeManagementCommandPort {

    override suspend fun createScope(command: ContractCreateScopeCommand): Either<ScopeContractError, CreateScopeResult> = createScopeHandler(command)

    override suspend fun updateScope(command: ContractUpdateScopeCommand): Either<ScopeContractError, UpdateScopeResult> = updateScopeHandler(
        UpdateScopeCommand(
            id = command.id,
            title = command.title,
            description = command.description,
        ),
    ).map { scopeResult ->
        UpdateScopeResult(
            id = scopeResult.id,
            title = scopeResult.title,
            description = scopeResult.description,
            parentId = scopeResult.parentId,
            canonicalAlias = scopeResult.canonicalAlias,
            createdAt = scopeResult.createdAt,
            updatedAt = scopeResult.updatedAt,
        )
    }

    override suspend fun deleteScope(command: ContractDeleteScopeCommand): Either<ScopeContractError, Unit> = deleteScopeHandler(
        DeleteScopeCommand(
            id = command.id,
            cascade = command.cascade,
        ),
    )

    override suspend fun addAlias(command: ContractAddAliasCommand): Either<ScopeContractError, Unit> = transactionManager.inTransaction {
        val scopeResult = getScopeByIdHandler(GetScopeById(command.scopeId))
        val existingAlias = scopeResult.fold(
            { error -> return@inTransaction Either.Left(error) },
            { scope ->
                when {
                    scope == null -> return@inTransaction Either.Left(
                        ScopeContractError.BusinessError.NotFound(
                            scopeId = command.scopeId,
                        ),
                    )
                    else -> scope.canonicalAlias
                }
            },
        )

        addAliasHandler(
            AddAliasCommand(
                existingAlias = existingAlias,
                newAlias = command.aliasName,
            ),
        )
    }

    override suspend fun removeAlias(command: ContractRemoveAliasCommand): Either<ScopeContractError, Unit> = removeAliasHandler(
        RemoveAliasCommand(
            aliasName = command.aliasName,
        ),
    )

    override suspend fun setCanonicalAlias(command: ContractSetCanonicalAliasCommand): Either<ScopeContractError, Unit> = transactionManager.inTransaction {
        val scopeResult = getScopeByIdHandler(GetScopeById(command.scopeId))
        val currentAlias = scopeResult.fold(
            { error -> return@inTransaction Either.Left(error) },
            { scope ->
                when {
                    scope == null -> return@inTransaction Either.Left(
                        ScopeContractError.BusinessError.NotFound(
                            scopeId = command.scopeId,
                        ),
                    )
                    else -> scope.canonicalAlias
                }
            },
        )

        setCanonicalAliasHandler(
            SetCanonicalAliasCommand(
                currentAlias = currentAlias,
                newCanonicalAlias = command.aliasName,
            ),
        )
    }

    override suspend fun renameAlias(command: ContractRenameAliasCommand): Either<ScopeContractError, Unit> = renameAliasHandler(
        RenameAliasCommand(
            currentAlias = command.oldAliasName,
            newAliasName = command.newAliasName,
        ),
    )
}
