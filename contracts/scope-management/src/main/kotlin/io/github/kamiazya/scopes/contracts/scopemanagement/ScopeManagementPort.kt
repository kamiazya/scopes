package io.github.kamiazya.scopes.contracts.scopemanagement

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.AddAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.RemoveAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.RenameAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetCanonicalAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetRootScopesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAliasesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListScopesWithAspectQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListScopesWithQueryQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateScopeResult

/**
 * Public contract for scope management operations.
 * All operations return Either for explicit error handling.
 */
public interface ScopeManagementPort {
    /**
     * Creates a new scope with the specified attributes.
     * @param command The command containing scope creation details
     * @return Either an error or the created scope result
     */
    public suspend fun createScope(command: CreateScopeCommand): Either<ScopeContractError, CreateScopeResult>

    /**
     * Updates an existing scope with new attributes.
     * @param command The command containing scope update details
     * @return Either an error or the updated scope result
     */
    public suspend fun updateScope(command: UpdateScopeCommand): Either<ScopeContractError, UpdateScopeResult>

    /**
     * Deletes a scope and all its children.
     * @param command The command containing the scope ID to delete
     * @return Either an error or Unit on successful deletion
     */
    public suspend fun deleteScope(command: DeleteScopeCommand): Either<ScopeContractError, Unit>

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
     * Adds a new alias to a scope.
     * @param command The command containing the scope ID and alias name
     * @return Either an error or Unit on success
     */
    public suspend fun addAlias(command: AddAliasCommand): Either<ScopeContractError, Unit>

    /**
     * Removes an alias from a scope.
     * @param command The command containing the scope ID and alias name
     * @return Either an error or Unit on success
     */
    public suspend fun removeAlias(command: RemoveAliasCommand): Either<ScopeContractError, Unit>

    /**
     * Sets the canonical alias for a scope.
     * @param command The command containing the scope ID and alias name
     * @return Either an error or Unit on success
     */
    public suspend fun setCanonicalAlias(command: SetCanonicalAliasCommand): Either<ScopeContractError, Unit>

    /**
     * Renames an alias.
     * @param command The command containing the old and new alias names
     * @return Either an error or Unit on success
     */
    public suspend fun renameAlias(command: RenameAliasCommand): Either<ScopeContractError, Unit>

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
