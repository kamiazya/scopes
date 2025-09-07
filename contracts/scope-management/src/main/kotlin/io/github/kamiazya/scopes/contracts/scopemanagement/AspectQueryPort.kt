package io.github.kamiazya.scopes.contracts.scopemanagement

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetAspectDefinitionQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAspectDefinitionsQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ValidateAspectValueQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.types.AspectDefinition

/**
 * Public contract for aspect query operations.
 * Provides a stable API for reading aspect definitions across bounded contexts.
 */
public interface AspectQueryPort {
    /**
     * Gets a specific aspect definition by key.
     * Returns null if not found (not an error case for queries).
     */
    public suspend fun getAspectDefinition(query: GetAspectDefinitionQuery): Either<ScopeContractError, AspectDefinition?>

    /**
     * Lists all aspect definitions.
     */
    public suspend fun listAspectDefinitions(query: ListAspectDefinitionsQuery): Either<ScopeContractError, List<AspectDefinition>>

    /**
     * Validates aspect values against their definitions.
     * Returns the validated values if successful.
     */
    public suspend fun validateAspectValue(query: ValidateAspectValueQuery): Either<ScopeContractError, List<String>>
}
