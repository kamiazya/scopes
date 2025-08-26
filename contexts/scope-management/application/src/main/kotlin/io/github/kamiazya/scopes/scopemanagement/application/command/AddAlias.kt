package io.github.kamiazya.scopes.scopemanagement.application.command

/**
 * Command to add a custom alias to an existing scope.
 *
 * @property scopeId The ID of the scope to add the alias to
 * @property aliasName The alias name to add
 */
data class AddAlias(val scopeId: String, val aliasName: String) : Command
