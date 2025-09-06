package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.AspectCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.CreateAspectDefinitionRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.DeleteAspectDefinitionRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.UpdateAspectDefinitionRequest
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
        aspectCommandPort.createAspectDefinition(CreateAspectDefinitionRequest(key = key, description = description, type = type))

    /**
     * Update an aspect definition.
     */
    suspend fun updateAspectDefinition(key: String, description: String? = null): Either<ScopeContractError, Unit> =
        aspectCommandPort.updateAspectDefinition(UpdateAspectDefinitionRequest(key = key, description = description))

    /**
     * Delete an aspect definition.
     */
    suspend fun deleteAspectDefinition(key: String): Either<ScopeContractError, Unit> =
        aspectCommandPort.deleteAspectDefinition(DeleteAspectDefinitionRequest(key))
}
