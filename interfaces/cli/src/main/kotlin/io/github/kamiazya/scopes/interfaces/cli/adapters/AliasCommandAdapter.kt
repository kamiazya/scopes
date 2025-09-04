package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
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
class AliasCommandAdapter(private val scopeManagementCommandPort: ScopeManagementCommandPort) {
    /**
     * Adds a new alias to a scope
     */
    suspend fun addAlias(scopeId: String, aliasName: String): Either<ScopeContractError, Unit> {
        val command = ContractAddAliasCommand(
            scopeId = scopeId,
            aliasName = aliasName,
        )
        return scopeManagementCommandPort.addAlias(command)
    }

    /**
     * Sets the canonical alias for a scope
     */
    suspend fun setCanonicalAlias(scopeId: String, aliasName: String): Either<ScopeContractError, Unit> {
        val command = ContractSetCanonicalAliasCommand(
            scopeId = scopeId,
            aliasName = aliasName,
        )
        return scopeManagementCommandPort.setCanonicalAlias(command)
    }

    /**
     * Removes an alias from a scope
     */
    suspend fun removeAlias(scopeId: String, aliasName: String): Either<ScopeContractError, Unit> {
        val command = ContractRemoveAliasCommand(
            scopeId = scopeId,
            aliasName = aliasName,
        )
        return scopeManagementCommandPort.removeAlias(command)
    }

    /**
     * Renames an alias
     */
    suspend fun renameAlias(oldAliasName: String, newAliasName: String): Either<ScopeContractError, Unit> {
        val command = ContractRenameAliasCommand(
            oldAliasName = oldAliasName,
            newAliasName = newAliasName,
        )
        return scopeManagementCommandPort.renameAlias(command)
    }
}
