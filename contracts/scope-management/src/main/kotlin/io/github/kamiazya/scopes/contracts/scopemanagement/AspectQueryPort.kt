package io.github.kamiazya.scopes.contracts.scopemanagement

import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.AspectContract
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.GetAspectDefinitionRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.ListAspectDefinitionsRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.ValidateAspectValueRequest

/**
 * Public contract for aspect query operations.
 * Provides a stable API for reading aspect definitions across bounded contexts.
 */
public interface AspectQueryPort {
    /**
     * Gets a specific aspect definition by key.
     */
    public suspend fun getAspectDefinition(query: GetAspectDefinitionRequest): AspectContract.GetAspectDefinitionResponse

    /**
     * Lists all aspect definitions.
     */
    public suspend fun listAspectDefinitions(query: ListAspectDefinitionsRequest): AspectContract.ListAspectDefinitionsResponse

    /**
     * Validates aspect values against their definitions.
     */
    public suspend fun validateAspectValue(query: ValidateAspectValueRequest): AspectContract.ValidateAspectValueResponse
}
