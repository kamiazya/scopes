package io.github.kamiazya.scopes.interfaces.cli.adapters.grpc

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.types.AspectDefinition
import io.github.kamiazya.scopes.interfaces.cli.transport.Transport

/**
 * gRPC-specific implementation of AspectQueryAdapter.
 */
class GrpcAspectQueryAdapter(
    private val transport: Transport
) {
    
    /**
     * Get an aspect definition by key.
     */
    suspend fun getAspectDefinition(key: String): Either<ScopeContractError, AspectDefinition?> {
        return transport.getAspectDefinition(key)
    }
    
    /**
     * List all aspect definitions.
     */
    suspend fun listAspectDefinitions(): Either<ScopeContractError, List<AspectDefinition>> {
        return transport.listAspectDefinitions()
    }
    
    /**
     * Validate aspect values against their definitions.
     */
    suspend fun validateAspectValue(key: String, values: List<String>): Either<ScopeContractError, List<String>> {
        return transport.validateAspectValue(key, values)
    }
}