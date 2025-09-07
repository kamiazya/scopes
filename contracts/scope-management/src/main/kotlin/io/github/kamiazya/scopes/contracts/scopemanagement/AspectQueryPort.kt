package io.github.kamiazya.scopes.contracts.scopemanagement

import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetAspectDefinitionQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAspectDefinitionsQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ValidateAspectValueQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.GetAspectDefinitionResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ListAspectDefinitionsResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ValidateAspectValueResult

/**
 * Public contract for aspect query operations.
 * Provides a stable API for reading aspect definitions across bounded contexts.
 */
public interface AspectQueryPort {
    /**
     * Gets a specific aspect definition by key.
     */
    public suspend fun getAspectDefinition(query: GetAspectDefinitionQuery): GetAspectDefinitionResult

    /**
     * Lists all aspect definitions.
     */
    public suspend fun listAspectDefinitions(query: ListAspectDefinitionsQuery): ListAspectDefinitionsResult

    /**
     * Validates aspect values against their definitions.
     */
    public suspend fun validateAspectValue(query: ValidateAspectValueQuery): ValidateAspectValueResult
}
