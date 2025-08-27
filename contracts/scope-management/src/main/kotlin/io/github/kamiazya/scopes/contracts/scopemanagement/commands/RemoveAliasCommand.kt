package io.github.kamiazya.scopes.contracts.scopemanagement.commands

/**
 * Command to remove an alias from a scope.
 *
 * @property scopeId The ID of the scope to remove the alias from
 * @property aliasName The alias name to remove
 */
public data class RemoveAliasCommand(public val scopeId: String, public val aliasName: String)
