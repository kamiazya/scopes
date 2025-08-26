package io.github.kamiazya.scopes.contracts.scopemanagement

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeQuery
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
    public suspend fun getChildren(query: GetChildrenQuery): Either<ScopeContractError, List<ScopeResult>>

    /**
     * Retrieves all root scopes (scopes without parent).
     * @return Either an error or the list of root scopes
     */
    public suspend fun getRootScopes(): Either<ScopeContractError, List<ScopeResult>>

    /**
     * Retrieves a scope by its alias name.
     * @param query The query containing the alias name
     * @return Either an error or the scope result (null if not found)
     */
    public suspend fun getScopeByAlias(query: GetScopeByAliasQuery): Either<ScopeContractError, ScopeResult>
}
