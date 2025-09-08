package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetRootScopesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAliasesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListScopesWithAspectQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListScopesWithQueryQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult

/**
 * Adapter for CLI queries to interact with Scope Management read operations.
 *
 * Following CQRS principles, this adapter handles only queries (read operations)
 * that retrieve data from the Scope Management bounded context without side effects.
 *
 * Key responsibilities:
 * - Adapts CLI query input to contract query port calls
 * - Optimizes read operations for CLI display and filtering needs
 * - Manages caching and performance concerns for read operations
 * - Provides view-optimized data transformation for CLI consumption
 */
class ScopeQueryAdapter(
    private val scopeManagementQueryPort: ScopeManagementQueryPort,
    // Future: Add other context query ports here
    // private val workspaceManagementQueryPort: WorkspaceManagementQueryPort?,
    // private val aiCollaborationQueryPort: AiCollaborationQueryPort?
) {
    /**
     * Retrieves a scope by ID
     */
    suspend fun getScopeById(id: String): Either<ScopeContractError, ScopeResult> {
        val query = GetScopeQuery(id = id)
        return scopeManagementQueryPort.getScope(query).fold(
            { error -> Either.Left(error) },
            { result -> result?.let { Either.Right(it) } ?: Either.Left(ScopeContractError.BusinessError.NotFound(id)) },
        )
    }

    /**
     * Retrieves a scope by alias name
     */
    suspend fun getScopeByAlias(aliasName: String): Either<ScopeContractError, ScopeResult> {
        val query = GetScopeByAliasQuery(aliasName = aliasName)
        return scopeManagementQueryPort.getScopeByAlias(query)
    }

    /**
     * Lists child scopes with pagination support
     */
    suspend fun listChildren(parentId: String, offset: Int = 0, limit: Int = 20): Either<ScopeContractError, ScopeListResult> {
        val query = GetChildrenQuery(parentId = parentId, offset = offset, limit = limit)
        return scopeManagementQueryPort.getChildren(query)
    }

    /**
     * Lists root scopes (scopes without parent) with pagination support
     */
    suspend fun listRootScopes(offset: Int = 0, limit: Int = 20): Either<ScopeContractError, ScopeListResult> {
        val query = GetRootScopesQuery(offset = offset, limit = limit)
        return scopeManagementQueryPort.getRootScopes(query)
    }

    /**
     * Lists all aliases for a specific scope
     */
    suspend fun listAliases(scopeId: String): Either<ScopeContractError, AliasListResult> {
        val query = ListAliasesQuery(scopeId = scopeId)
        return scopeManagementQueryPort.listAliases(query)
    }

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
        val query = ListScopesWithAspectQuery(
            aspectKey = aspectKey,
            aspectValue = aspectValue,
            parentId = parentId,
            offset = offset,
            limit = limit,
        )
        return scopeManagementQueryPort.listScopesWithAspect(query)
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
        val query = ListScopesWithQueryQuery(
            aspectQuery = aspectQuery,
            parentId = parentId,
            offset = offset,
            limit = limit,
        )
        return scopeManagementQueryPort.listScopesWithQuery(query)
    }

    /**
     * Searches scopes by title or description (convenience method)
     * Uses aspect query with search expressions internally
     */
    suspend fun searchScopes(searchTerm: String, parentId: String? = null, offset: Int = 0, limit: Int = 20): Either<ScopeContractError, List<ScopeResult>> {
        // Build search query for title and description
        val query = "title CONTAINS \"$searchTerm\" OR description CONTAINS \"$searchTerm\""
        return listScopesWithQuery(query, parentId, offset, limit)
    }

    /**
     * Gets scope hierarchy path from root to the specified scope
     * This method demonstrates read-only aggregation of multiple queries
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
