package io.github.kamiazya.scopes.contracts.scopemanagement.commands

/**
 * Command to add a new alias to a scope.
 *
 * @property scopeId The ID of the scope to add the alias to
 * @property aliasName The alias name to add
 */
public data class AddAliasCommand(public val scopeId: String, public val aliasName: String)
