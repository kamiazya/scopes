package io.github.kamiazya.scopes.interfaces.cli.adapters.grpc

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.interfaces.cli.transport.Transport

/**
 * gRPC-specific implementation of ScopeQueryAdapter.
 *
 * This adapter delegates scope queries to the Transport layer
 * when using gRPC transport.
 */
class GrpcScopeQueryAdapter(private val transport: Transport) {

    /**
     * Retrieves a scope by ID
     */
    suspend fun getScopeById(id: String): Either<ScopeContractError, ScopeResult> = transport.getScope(id).fold(
        { error -> Either.Left(error) },
        { result -> result?.let { Either.Right(it) } ?: Either.Left(ScopeContractError.BusinessError.NotFound(id)) },
    )

    /**
     * Retrieves a scope by alias name
     */
    suspend fun getScopeByAlias(aliasName: String): Either<ScopeContractError, ScopeResult> {
        val resolveResult = transport.resolveAlias(aliasName)
        return when (resolveResult) {
            is Either.Left -> resolveResult
            is Either.Right -> getScopeById(resolveResult.value.scopeId)
        }
    }

    /**
     * Lists child scopes with pagination support
     */
    suspend fun listChildren(parentId: String, offset: Int = 0, limit: Int = 20): Either<ScopeContractError, ScopeListResult> =
        transport.getChildren(parentId, false)

    /**
     * Lists root scopes (scopes without parent) with pagination support
     */
    suspend fun listRootScopes(offset: Int = 0, limit: Int = 20): Either<ScopeContractError, ScopeListResult> = transport.getRootScopes()

    /**
     * Lists all aliases for a specific scope
     */
    suspend fun listAliases(scopeId: String): Either<ScopeContractError, AliasListResult> = transport.listAliases(scopeId)

    /**
     * Lists scopes filtered by a specific aspect key-value pair
     */
    suspend fun listScopesWithAspect(
        aspectKey: String,
        aspectValue: String,
        parentId: String? = null,
        offset: Int = 0,
        limit: Int = 20,
    ): Either<ScopeContractError, List<ScopeResult>> {
        // For gRPC transport, use basic listing and filter client-side for now
        return transport.listScopes().map { result ->
            result.scopes.filter { scope ->
                scope.aspects[aspectKey]?.contains(aspectValue) == true
            }
        }
    }

    /**
     * Lists scopes filtered by advanced aspect query with support for complex expressions
     */
    suspend fun listScopesWithQuery(
        aspectQuery: String,
        parentId: String? = null,
        offset: Int = 0,
        limit: Int = 20,
    ): Either<ScopeContractError, List<ScopeResult>> {
        // For gRPC transport, not yet implemented - return empty list
        return Either.Right(emptyList())
    }

    /**
     * Searches scopes by title or description (convenience method)
     */
    suspend fun searchScopes(searchTerm: String, parentId: String? = null, offset: Int = 0, limit: Int = 20): Either<ScopeContractError, List<ScopeResult>> =
        transport.listScopes().map { result ->
            result.scopes.filter { scope ->
                scope.title.contains(searchTerm, ignoreCase = true) ||
                    scope.description?.contains(searchTerm, ignoreCase = true) == true
            }
        }

    /**
     * Gets scope hierarchy path from root to the specified scope
     */
    suspend fun getScopeHierarchyPath(scopeId: String): Either<ScopeContractError, List<ScopeResult>> {
        val path = mutableListOf<ScopeResult>()
        var currentScopeId: String? = scopeId

        while (currentScopeId != null) {
            val scopeResult = getScopeById(currentScopeId)
            when (scopeResult) {
                is Either.Left -> return scopeResult
                is Either.Right -> {
                    val scope = scopeResult.value
                    path.add(0, scope) // Add to beginning to build path from root
                    currentScopeId = scope.parentId
                }
            }
        }

        return Either.Right(path)
    }
}
