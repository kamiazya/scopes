package io.github.kamiazya.scopes.interfaces.cli.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.AspectQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetAspectDefinitionQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAspectDefinitionsQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ValidateAspectValueQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.GetAspectDefinitionResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ListAspectDefinitionsResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ValidateAspectValueResult

/**
 * Query adapter for aspect-related CLI queries.
 * Maps between CLI queries and aspect query port.
 */
class AspectQueryAdapter(private val aspectQueryPort: AspectQueryPort) {
    /**
     * Get an aspect definition by key.
     */
    suspend fun getAspectDefinition(key: String): GetAspectDefinitionResult = aspectQueryPort.getAspectDefinition(GetAspectDefinitionQuery(key))

    /**
     * List all aspect definitions.
     */
    suspend fun listAspectDefinitions(): ListAspectDefinitionsResult = aspectQueryPort.listAspectDefinitions(ListAspectDefinitionsQuery)

    /**
     * Validate aspect values against their definitions.
     */
    suspend fun validateAspectValue(key: String, values: List<String>): ValidateAspectValueResult =
        aspectQueryPort.validateAspectValue(ValidateAspectValueQuery(key, values))
}
