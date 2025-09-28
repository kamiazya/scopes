package io.github.kamiazya.scopes.interfaces.cli.adapters.grpc

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.interfaces.cli.transport.Transport

/**
 * gRPC-specific implementation of AliasQueryAdapter.
 */
class GrpcAliasQueryAdapter(
    private val transport: Transport
) {
    
    /**
     * Lists all aliases for a scope
     */
    suspend fun listAliases(scopeId: String): Either<ScopeContractError, AliasListResult> {
        return transport.listAliases(scopeId)
    }
}