package io.github.kamiazya.scopes.interfaces.cli.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.AspectQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.AspectContract
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.GetAspectDefinitionRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.ListAspectDefinitionsRequest
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.ValidateAspectValueRequest

/**
 * Query adapter for aspect-related CLI queries.
 * Maps between CLI queries and aspect query port.
 */
class AspectQueryAdapter(private val aspectQueryPort: AspectQueryPort) {
    /**
     * Get an aspect definition by key.
     */
    suspend fun getAspectDefinition(key: String): AspectContract.GetAspectDefinitionResponse =
        aspectQueryPort.getAspectDefinition(GetAspectDefinitionRequest(key))

    /**
     * List all aspect definitions.
     */
    suspend fun listAspectDefinitions(): AspectContract.ListAspectDefinitionsResponse = aspectQueryPort.listAspectDefinitions(ListAspectDefinitionsRequest)

    /**
     * Validate aspect values against their definitions.
     */
    suspend fun validateAspectValue(key: String, values: List<String>): AspectContract.ValidateAspectValueResponse =
        aspectQueryPort.validateAspectValue(ValidateAspectValueRequest(key, values))
}
