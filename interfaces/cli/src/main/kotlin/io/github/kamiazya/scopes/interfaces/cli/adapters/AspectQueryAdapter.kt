package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.AspectQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetAspectDefinitionQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAspectDefinitionsQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ValidateAspectValueQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.types.AspectDefinition

/**
 * Query adapter for aspect-related CLI queries.
 * Maps between CLI queries and aspect query port.
 */
class AspectQueryAdapter(private val aspectQueryPort: AspectQueryPort) {
    /**
     * Get an aspect definition by key.
     */
    suspend fun getAspectDefinition(key: String): Either<ScopeContractError, AspectDefinition?> =
        aspectQueryPort.getAspectDefinition(GetAspectDefinitionQuery(key))

    /**
     * List all aspect definitions.
     */
    suspend fun listAspectDefinitions(): Either<ScopeContractError, List<AspectDefinition>> = aspectQueryPort.listAspectDefinitions(ListAspectDefinitionsQuery)

    /**
     * Validate aspect values against their definitions.
     */
    suspend fun validateAspectValue(key: String, values: List<String>): Either<ScopeContractError, List<String>> =
        aspectQueryPort.validateAspectValue(ValidateAspectValueQuery(key, values))
}
