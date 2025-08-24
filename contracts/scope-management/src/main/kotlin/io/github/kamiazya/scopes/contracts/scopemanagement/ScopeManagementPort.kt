package io.github.kamiazya.scopes.contracts.scopemanagement

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetRootScopesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateScopeResult

/**
 * Public contract for scope management operations.
 * All operations return Either for explicit error handling.
 */
public interface ScopeManagementPort {
    public suspend fun createScope(command: CreateScopeCommand): Either<ScopeContractError, CreateScopeResult>
    public suspend fun updateScope(command: UpdateScopeCommand): Either<ScopeContractError, UpdateScopeResult>
    public suspend fun deleteScope(command: DeleteScopeCommand): Either<ScopeContractError, Unit>
    public suspend fun getScope(query: GetScopeQuery): Either<ScopeContractError, ScopeResult?>
    public suspend fun getChildren(query: GetChildrenQuery): Either<ScopeContractError, List<ScopeResult>>
    public suspend fun getRootScopes(query: GetRootScopesQuery): Either<ScopeContractError, List<ScopeResult>>
}
