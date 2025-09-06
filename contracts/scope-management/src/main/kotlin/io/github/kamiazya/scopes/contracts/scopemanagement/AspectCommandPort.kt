package io.github.kamiazya.scopes.contracts.scopemanagement

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.CreateAspectDefinitionCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.DeleteAspectDefinitionCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.UpdateAspectDefinitionCommand
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
    public suspend fun createAspectDefinition(command: CreateAspectDefinitionCommand): Either<ScopeContractError, Unit>

    /**
     * Updates an existing aspect definition.
     * Returns minimal success indicator following CQRS principles.
     */
    public suspend fun updateAspectDefinition(command: UpdateAspectDefinitionCommand): Either<ScopeContractError, Unit>

    /**
     * Deletes an aspect definition.
     * Returns minimal success indicator following CQRS principles.
     */
    public suspend fun deleteAspectDefinition(command: DeleteAspectDefinitionCommand): Either<ScopeContractError, Unit>
}
