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
interface ScopeManagementPort {
    suspend fun createScope(command: CreateScopeCommand): Either<ScopeContractError, CreateScopeResult>
    suspend fun updateScope(command: UpdateScopeCommand): Either<ScopeContractError, UpdateScopeResult>
    suspend fun deleteScope(command: DeleteScopeCommand): Either<ScopeContractError, Unit>
    suspend fun getScope(query: GetScopeQuery): Either<ScopeContractError, ScopeResult?>
    suspend fun getChildren(query: GetChildrenQuery): Either<ScopeContractError, List<ScopeResult>>
    suspend fun getRootScopes(query: GetRootScopesQuery): Either<ScopeContractError, List<ScopeResult>>
}
