package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.AspectCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.CreateAspectDefinitionCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.DeleteAspectDefinitionCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.UpdateAspectDefinitionCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError

/**
 * Command adapter for aspect-related CLI commands.
 * Maps between CLI commands and aspect command port.
 */
class AspectCommandAdapter(private val aspectCommandPort: AspectCommandPort) {
    /**
     * Define a new aspect.
     */
    suspend fun defineAspect(key: String, description: String, type: String): Either<ScopeContractError, Unit> =
        aspectCommandPort.createAspectDefinition(CreateAspectDefinitionCommand(key = key, description = description, type = type))

    /**
     * Update an aspect definition.
     */
    suspend fun updateAspectDefinition(key: String, description: String? = null): Either<ScopeContractError, Unit> =
        aspectCommandPort.updateAspectDefinition(UpdateAspectDefinitionCommand(key = key, description = description))

    /**
     * Delete an aspect definition.
     */
    suspend fun deleteAspectDefinition(key: String): Either<ScopeContractError, Unit> =
        aspectCommandPort.deleteAspectDefinition(DeleteAspectDefinitionCommand(key))
}
