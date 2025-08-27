package io.github.kamiazya.scopes.scopemanagement.application.command

/**
 * Command to rename an existing alias.
 *
 * @property currentAlias The current alias name to rename
 * @property newAliasName The new name for the alias
 */
data class RenameAlias(val currentAlias: String, val newAliasName: String) : Command
