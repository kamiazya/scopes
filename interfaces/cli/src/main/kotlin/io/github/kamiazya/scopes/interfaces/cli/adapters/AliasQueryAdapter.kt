package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAliasesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult

/**
 * Adapter for CLI alias query operations
 *
 * This adapter provides read-only alias operations,
 * bridging the CLI layer with the query port for alias retrieval.
 */
class AliasQueryAdapter(private val scopeManagementQueryPort: ScopeManagementQueryPort) {
    /**
     * Lists all aliases for a scope
     */
    suspend fun listAliases(scopeId: String): Either<ScopeContractError, AliasListResult> {
        val query = ListAliasesQuery(scopeId = scopeId)
        return scopeManagementQueryPort.listAliases(query)
    }
}
