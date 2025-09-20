package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.contracts.scopemanagement.AspectCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateAspectDefinitionCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteAspectDefinitionCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateAspectDefinitionCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.DefineAspectCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect.DefineAspectHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect.DeleteAspectDefinitionHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect.UpdateAspectDefinitionHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.DeleteAspectDefinitionCommand as AppDeleteAspectDefinitionCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.UpdateAspectDefinitionCommand as AppUpdateAspectDefinitionCommand

/**
 * Command port adapter implementation for Aspect operations.
 * Handles command operations that modify aspect definitions.
 */
public class AspectCommandPortAdapter(
    private val defineAspectHandler: DefineAspectHandler,
    private val updateAspectDefinitionHandler: UpdateAspectDefinitionHandler,
    private val deleteAspectDefinitionHandler: DeleteAspectDefinitionHandler,
) : AspectCommandPort {

    override suspend fun createAspectDefinition(command: CreateAspectDefinitionCommand): Either<ScopeContractError, Unit> {
        // Validate aspect type first
        val aspectType = when (command.type.lowercase(java.util.Locale.ROOT)) {
            "text" -> io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType.Text
            "numeric" -> io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType.Numeric
            "boolean" -> io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType.BooleanType
            "duration" -> io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType.Duration
            "ordered" -> io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType.Ordered(emptyList())
            else -> {
                return ScopeContractError.InputError.ValidationFailure(
                    field = "type",
                    value = command.type,
                    constraint = ScopeContractError.ValidationConstraint.InvalidValue(
                        expectedValues = listOf("text", "numeric", "boolean", "duration", "ordered"),
                        actualValue = command.type,
                    ),
                ).left()
            }
        }

        val result = defineAspectHandler(
            DefineAspectCommand(
                key = command.key,
                description = command.description,
                type = aspectType,
            ),
        )

        return result.fold(
            ifLeft = { error -> error.left() },
            ifRight = { Unit.right() },
        )
    }

    override suspend fun updateAspectDefinition(command: UpdateAspectDefinitionCommand): Either<ScopeContractError, Unit> {
        val result = updateAspectDefinitionHandler(
            AppUpdateAspectDefinitionCommand(
                key = command.key,
                description = command.description,
            ),
        )

        return result.fold(
            ifLeft = { error -> error.left() },
            ifRight = { Unit.right() },
        )
    }

    override suspend fun deleteAspectDefinition(command: DeleteAspectDefinitionCommand): Either<ScopeContractError, Unit> {
        val result = deleteAspectDefinitionHandler(AppDeleteAspectDefinitionCommand(command.key))

        return result.fold(
            ifLeft = { error -> error.left() },
            ifRight = { Unit.right() },
        )
    }
}
