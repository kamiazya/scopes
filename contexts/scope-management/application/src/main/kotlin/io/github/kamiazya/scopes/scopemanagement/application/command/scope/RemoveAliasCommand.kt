package io.github.kamiazya.scopes.scopemanagement.application.command.scope

/**
 * Command to remove an alias from a scope.
 * Cannot remove canonical aliases - use SetCanonicalAlias instead.
 *
 * Follows CQRS naming convention: all commands should end with 'Command' suffix.
 *
 * @property aliasName The alias name to remove
 */
data class RemoveAliasCommand(val aliasName: String)
