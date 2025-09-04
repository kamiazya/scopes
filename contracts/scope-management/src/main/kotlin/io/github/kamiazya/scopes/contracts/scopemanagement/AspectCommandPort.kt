package io.github.kamiazya.scopes.contracts.scopemanagement

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.CreateAspectDefinitionRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.DeleteAspectDefinitionRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.UpdateAspectDefinitionRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError

/**
 * Public contract for aspect command operations.
 * Provides a stable API for modifying aspect definitions across bounded contexts.
 */
public interface AspectCommandPort {
    /**
     * Creates a new aspect definition.
     * Returns minimal success indicator following CQRS principles.
     */
    public suspend fun createAspectDefinition(command: CreateAspectDefinitionRequest): Either<ScopeContractError, Unit>

    /**
     * Updates an existing aspect definition.
     * Returns minimal success indicator following CQRS principles.
     */
    public suspend fun updateAspectDefinition(command: UpdateAspectDefinitionRequest): Either<ScopeContractError, Unit>

    /**
     * Deletes an aspect definition.
     * Returns minimal success indicator following CQRS principles.
     */
    public suspend fun deleteAspectDefinition(command: DeleteAspectDefinitionRequest): Either<ScopeContractError, Unit>
}
