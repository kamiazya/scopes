package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementPort
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAliasesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.AddAliasCommand as ContractAddAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.RemoveAliasCommand as ContractRemoveAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.RenameAliasCommand as ContractRenameAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetCanonicalAliasCommand as ContractSetCanonicalAliasCommand

/**
 * Adapter for CLI alias commands to interact with Scope Management
 *
 * This adapter provides a focused interface for alias management operations,
 * bridging the CLI layer with the contract layer for alias-specific functionality.
 */
class AliasCommandAdapter(private val scopeManagementPort: ScopeManagementPort) {
    /**
     * Adds a new alias to a scope
     */
    suspend fun addAlias(scopeId: String, aliasName: String): Either<ScopeContractError, Unit> {
        val command = ContractAddAliasCommand(
            scopeId = scopeId,
            aliasName = aliasName,
        )
        return scopeManagementPort.addAlias(command)
    }

    /**
     * Lists all aliases for a scope
     */
    suspend fun listAliases(scopeId: String): Either<ScopeContractError, AliasListResult> {
        val query = ListAliasesQuery(scopeId = scopeId)
        return scopeManagementPort.listAliases(query)
    }

    /**
     * Sets the canonical alias for a scope
     */
    suspend fun setCanonicalAlias(scopeId: String, aliasName: String): Either<ScopeContractError, Unit> {
        val command = ContractSetCanonicalAliasCommand(
            scopeId = scopeId,
            aliasName = aliasName,
        )
        return scopeManagementPort.setCanonicalAlias(command)
    }

    /**
     * Removes an alias from a scope
     */
    suspend fun removeAlias(scopeId: String, aliasName: String): Either<ScopeContractError, Unit> {
        val command = ContractRemoveAliasCommand(
            scopeId = scopeId,
            aliasName = aliasName,
        )
        return scopeManagementPort.removeAlias(command)
    }

    /**
     * Renames an alias
     */
    suspend fun renameAlias(oldAliasName: String, newAliasName: String): Either<ScopeContractError, Unit> {
        val command = ContractRenameAliasCommand(
            oldAliasName = oldAliasName,
            newAliasName = newAliasName,
        )
        return scopeManagementPort.renameAlias(command)
    }
}
