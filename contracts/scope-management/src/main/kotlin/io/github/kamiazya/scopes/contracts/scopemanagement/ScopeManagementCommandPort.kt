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
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateScopeResult

/**
 * Public contract for scope management write operations (Commands).
 * Following CQRS principles, this port handles only operations that modify state.
 * All operations return Either for explicit error handling.
 */
public interface ScopeManagementCommandPort {
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
}
