package io.github.kamiazya.scopes.contracts.scopemanagement

import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.AspectContract
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.GetAspectDefinitionQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.ListAspectDefinitionsQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.ValidateAspectValueQuery

/**
 * Public contract for aspect query operations.
 * Provides a stable API for reading aspect definitions across bounded contexts.
 */
public interface AspectQueryPort {
    /**
     * Gets a specific aspect definition by key.
     */
    public suspend fun getAspectDefinition(query: GetAspectDefinitionQuery): AspectContract.GetAspectDefinitionResponse

    /**
     * Lists all aspect definitions.
     */
    public suspend fun listAspectDefinitions(query: ListAspectDefinitionsQuery): AspectContract.ListAspectDefinitionsResponse

    /**
     * Validates aspect values against their definitions.
     */
    public suspend fun validateAspectValue(query: ValidateAspectValueQuery): AspectContract.ValidateAspectValueResponse
}
