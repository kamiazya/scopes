package io.github.kamiazya.scopes.interfaces.cli.adapters.grpc

import arrow.core.Either
import arrow.core.right
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.types.ContextView

/**
 * gRPC-specific implementation of ContextQueryAdapter.
 * 
 * This provides the same interface as ContextQueryAdapter but delegates
 * to stub implementations since context management is not yet supported in gRPC transport.
 */
class GrpcContextQueryAdapter {
    
    /**
     * List all context views.
     */
    suspend fun listContextViews(): Either<ScopeContractError, List<ContextView>> {
        // For gRPC transport, context management is not supported yet
        return emptyList<ContextView>().right()
    }
    
    /**
     * Get a specific context view by key.
     */
    suspend fun getContextView(key: String): Either<ScopeContractError, ContextView?> {
        // For gRPC transport, context management is not supported yet
        return null.right()
    }
    
    /**
     * Get the currently active context.
     */
    suspend fun getCurrentContext(): Either<ScopeContractError, ContextView?> {
        // For gRPC transport, context management is not supported yet
        return null.right()
    }
}