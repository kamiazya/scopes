package io.github.kamiazya.scopes.scopemanagement.application.command.scope

/**
 * Command to rename an existing alias.
 *
 * Follows CQRS naming convention: all commands should end with 'Command' suffix.
 *
 * @property currentAlias The current alias name to rename
 * @property newAliasName The new name for the alias
 */
data class RenameAliasCommand(val currentAlias: String, val newAliasName: String)
