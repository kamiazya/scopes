package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.context.CreateContextViewRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.DeleteContextViewRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.SetActiveContextRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.context.UpdateContextViewRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.CreateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.DeleteContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.UpdateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.context.CreateContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.context.DeleteContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.context.UpdateContextViewHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.ActiveContextService

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

    override suspend fun createContextView(command: CreateContextViewRequest): Either<ScopeContractError, Unit> {
        val result = createContextViewHandler(
            CreateContextViewCommand(
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

    override suspend fun updateContextView(command: UpdateContextViewRequest): Either<ScopeContractError, Unit> {
        val result = updateContextViewHandler(
            UpdateContextViewCommand(
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

    override suspend fun deleteContextView(command: DeleteContextViewRequest): Either<ScopeContractError, Unit> {
        val result = deleteContextViewHandler(DeleteContextViewCommand(command.key))

        return result.fold(
            ifLeft = { error -> errorMapper.mapToContractError(error).left() },
            ifRight = { Unit.right() },
        )
    }

    override suspend fun setActiveContext(command: SetActiveContextRequest): Either<ScopeContractError, Unit> {
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
