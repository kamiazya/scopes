package io.github.kamiazya.scopes.scopemanagement.application.command

/**
 * Command to add a custom alias to an existing scope.
 * Uses an existing alias to identify the scope.
 *
 * @property existingAlias An existing alias of the scope
 * @property newAlias The new alias name to add
 */
data class AddAlias(val existingAlias: String, val newAlias: String) : Command
