package io.github.kamiazya.scopes.contracts.scopemanagement.commands

/**
 * Command to rename an alias.
 *
 * @property oldAliasName The current alias name
 * @property newAliasName The new alias name
 */
public data class RenameAliasCommand(public val oldAliasName: String, public val newAliasName: String)
