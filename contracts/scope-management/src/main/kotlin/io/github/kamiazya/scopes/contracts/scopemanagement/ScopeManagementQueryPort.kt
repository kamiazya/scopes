package io.github.kamiazya.scopes.contracts.scopemanagement

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetRootScopesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAliasesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListScopesWithAspectQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListScopesWithQueryQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import org.jmolecules.architecture.hexagonal.PrimaryPort

/**
 * Public contract for scope management read operations (Queries).
 * Following CQRS principles, this port handles only operations that read data without side effects.
 * All operations return Either for explicit error handling.
 *
 * Marked with @PrimaryPort to indicate this is a driving port in hexagonal architecture,
 * representing the application's query API exposed to external adapters (CLI, API, MCP).
 */
@PrimaryPort
public interface ScopeManagementQueryPort {
    /**
     * Retrieves a single scope by its ID.
     * @param query The query containing the scope ID
     * @return Either an error or the scope result (null if not found)
     */
    public suspend fun getScope(query: GetScopeQuery): Either<ScopeContractError, ScopeResult?>

    /**
     * Retrieves all child scopes of a given parent scope.
     * @param query The query containing the parent scope ID
     * @return Either an error or the list of child scopes
     */
    public suspend fun getChildren(
        query: GetChildrenQuery,
    ): Either<ScopeContractError, io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult>

    /**
     * Retrieves root scopes (scopes without parent) with deterministic ordering.
     * Pagination is controlled by the query (offset ≥ 0, limit > 0).
     * @return Either an error or the list of root scopes for the requested page
     */
    public suspend fun getRootScopes(
        query: GetRootScopesQuery,
    ): Either<ScopeContractError, io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult>

    /**
     * Retrieves a scope by its alias name.
     * @param query The query containing the alias name
     * @return Either an error or the scope result (null if not found)
     */
    public suspend fun getScopeByAlias(query: GetScopeByAliasQuery): Either<ScopeContractError, ScopeResult>

    /**
     * Retrieves all aliases for a specific scope.
     * @param query The query containing the scope ID
     * @return Either an error or the alias list result
     */
    public suspend fun listAliases(query: ListAliasesQuery): Either<ScopeContractError, AliasListResult>

    /**
     * Lists scopes filtered by aspect criteria.
     * @param query The query containing aspect filter criteria
     * @return Either an error or the list of scopes matching the criteria
     */
    public suspend fun listScopesWithAspect(query: ListScopesWithAspectQuery): Either<ScopeContractError, List<ScopeResult>>

    /**
     * Lists scopes filtered by advanced aspect query.
     * Supports comparison operators (=, !=, >, >=, <, <=) and logical operators (AND, OR, NOT).
     * @param query The query containing the aspect query string
     * @return Either an error or the list of scopes matching the query
     */
    public suspend fun listScopesWithQuery(query: ListScopesWithQueryQuery): Either<ScopeContractError, List<ScopeResult>>
}
