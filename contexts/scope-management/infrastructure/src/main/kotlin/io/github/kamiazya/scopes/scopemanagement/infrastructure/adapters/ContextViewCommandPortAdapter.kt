package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetActiveContextCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.scopemanagement.application.handler.command.CreateContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.command.DeleteContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.command.UpdateContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.ActiveContextService
import io.github.kamiazya.scopes.scopemanagement.application.command.context.CreateContextViewCommand as AppCreateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.context.DeleteContextViewCommand as AppDeleteContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.context.UpdateContextViewCommand as AppUpdateContextViewCommand

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

    override suspend fun createContextView(command: CreateContextViewCommand): Either<ScopeContractError, Unit> {
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

    override suspend fun updateContextView(command: UpdateContextViewCommand): Either<ScopeContractError, Unit> {
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

    override suspend fun deleteContextView(command: DeleteContextViewCommand): Either<ScopeContractError, Unit> {
        val result = deleteContextViewHandler(AppDeleteContextViewCommand(command.key))

        return result.fold(
            ifLeft = { error -> errorMapper.mapToContractError(error).left() },
            ifRight = { Unit.right() },
        )
    }

    override suspend fun setActiveContext(command: SetActiveContextCommand): Either<ScopeContractError, Unit> {
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
