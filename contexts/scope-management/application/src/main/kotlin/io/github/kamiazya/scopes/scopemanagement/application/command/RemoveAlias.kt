package io.github.kamiazya.scopes.scopemanagement.application.command

/**
 * Command to remove an alias from a scope.
 * Cannot remove canonical aliases - use SetCanonicalAlias instead.
 *
 * @property aliasName The alias name to remove
 */
data class RemoveAlias(val aliasName: String) : Command
