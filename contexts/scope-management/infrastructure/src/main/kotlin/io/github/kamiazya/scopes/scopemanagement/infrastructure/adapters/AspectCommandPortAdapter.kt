package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.contracts.scopemanagement.AspectCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.CreateAspectDefinitionRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.DeleteAspectDefinitionRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.UpdateAspectDefinitionRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.DefineAspectCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.DeleteAspectDefinitionCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.UpdateAspectDefinitionCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect.DefineAspectHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect.DeleteAspectDefinitionHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect.UpdateAspectDefinitionHandler
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError

/**
 * Command port adapter implementation for Aspect operations.
 * Handles command operations that modify aspect definitions.
 */
public class AspectCommandPortAdapter(
    private val defineAspectHandler: DefineAspectHandler,
    private val updateAspectDefinitionHandler: UpdateAspectDefinitionHandler,
    private val deleteAspectDefinitionHandler: DeleteAspectDefinitionHandler,
) : AspectCommandPort {

    override suspend fun createAspectDefinition(command: CreateAspectDefinitionRequest): Either<ScopeContractError, Unit> {
        val result = defineAspectHandler(
            DefineAspectCommand(
                key = command.key,
                description = command.description,
                type = when (command.type.lowercase()) {
                    "text" -> io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType.Text
                    "numeric" -> io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType.Numeric
                    "boolean" -> io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType.BooleanType
                    "duration" -> io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType.Duration
                    else -> io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType.Text
                },
            ),
        )

        return result.fold(
            ifLeft = { error -> mapScopesErrorToScopeContractError(error).left() },
            ifRight = { Unit.right() },
        )
    }

    override suspend fun updateAspectDefinition(command: UpdateAspectDefinitionRequest): Either<ScopeContractError, Unit> {
        val result = updateAspectDefinitionHandler(
            UpdateAspectDefinitionCommand(
                key = command.key,
                description = command.description,
            ),
        )

        return result.fold(
            ifLeft = { error -> mapScopesErrorToScopeContractError(error).left() },
            ifRight = { Unit.right() },
        )
    }

    override suspend fun deleteAspectDefinition(command: DeleteAspectDefinitionRequest): Either<ScopeContractError, Unit> {
        val result = deleteAspectDefinitionHandler(DeleteAspectDefinitionCommand(command.key))

        return result.fold(
            ifLeft = { error -> mapScopesErrorToScopeContractError(error).left() },
            ifRight = { Unit.right() },
        )
    }

    /**
     * Maps domain errors to contract layer errors following CQRS principles.
     */
    private fun mapScopesErrorToScopeContractError(error: ScopesError): ScopeContractError = when (error) {
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError.NotFound ->
            ScopeContractError.BusinessError.NotFound(error.message)
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError.AlreadyExists ->
            ScopeContractError.BusinessError.DuplicateAlias(error.message)
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError.ValidationFailed ->
            ScopeContractError.InputError.InvalidTitle(error.message, ScopeContractError.TitleValidationFailure.Empty)
        else -> ScopeContractError.SystemError.ServiceUnavailable("AspectService")
    }
}
