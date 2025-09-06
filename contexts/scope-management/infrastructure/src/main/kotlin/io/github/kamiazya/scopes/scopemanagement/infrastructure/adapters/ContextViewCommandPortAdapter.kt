package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.context.CreateContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.context.DeleteContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.context.UpdateContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.ActiveContextService
import io.github.kamiazya.scopes.contracts.scopemanagement.context.CreateContextViewCommand as ContractCreateContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.context.DeleteContextViewCommand as ContractDeleteContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.context.SetActiveContextCommand as ContractSetActiveContextCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.context.UpdateContextViewCommand as ContractUpdateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.CreateContextViewCommand as AppCreateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.DeleteContextViewCommand as AppDeleteContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.UpdateContextViewCommand as AppUpdateContextViewCommand

/**
 * Command port adapter implementation for ContextView operations.
 * Handles command operations that modify context views.
 */
public class ContextViewCommandPortAdapter(
    private val createContextViewHandler: CreateContextViewHandler,
    private val updateContextViewHandler: UpdateContextViewHandler,
    private val deleteContextViewHandler: DeleteContextViewHandler,
    private val activeContextService: ActiveContextService,
    private val errorMapper: ErrorMapper,
    private val applicationErrorMapper: ApplicationErrorMapper,
) : ContextViewCommandPort {

    override suspend fun createContextView(command: ContractCreateContextViewCommand): Either<ScopeContractError, Unit> {
        val result = createContextViewHandler(
            AppCreateContextViewCommand(
                key = command.key,
                name = command.name,
                filter = command.filter,
                description = command.description,
            ),
        )

        return result.fold(
            ifLeft = { error -> errorMapper.mapToContractError(error).left() },
            ifRight = { Unit.right() },
        )
    }

    override suspend fun updateContextView(command: ContractUpdateContextViewCommand): Either<ScopeContractError, Unit> {
        val result = updateContextViewHandler(
            AppUpdateContextViewCommand(
                key = command.key,
                name = command.name,
                filter = command.filter,
                description = command.description,
            ),
        )

        return result.fold(
            ifLeft = { error -> errorMapper.mapToContractError(error).left() },
            ifRight = { Unit.right() },
        )
    }

    override suspend fun deleteContextView(command: ContractDeleteContextViewCommand): Either<ScopeContractError, Unit> {
        val result = deleteContextViewHandler(AppDeleteContextViewCommand(command.key))

        return result.fold(
            ifLeft = { error -> errorMapper.mapToContractError(error).left() },
            ifRight = { Unit.right() },
        )
    }

    override suspend fun setActiveContext(command: ContractSetActiveContextCommand): Either<ScopeContractError, Unit> {
        val result = activeContextService.switchToContextByKey(command.key)

        return result.fold(
            ifLeft = { error -> applicationErrorMapper.mapToContractError(error).left() },
            ifRight = { Unit.right() },
        )
    }

    override suspend fun clearActiveContext(): Either<ScopeContractError, Unit> {
        val result = activeContextService.clearActiveContext()

        return result.fold(
            ifLeft = { error -> applicationErrorMapper.mapToContractError(error).left() },
            ifRight = { Unit.right() },
        )
    }
}
