package io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope

/**
 * Command to add a custom alias to an existing scope.
 * Uses an existing alias to identify the scope.
 * Named with 'Command' suffix to follow CQRS naming conventions.
 *
 * @property existingAlias An existing alias of the scope
 * @property newAlias The new alias name to add
 */
data class AddAliasCommand(val existingAlias: String, val newAlias: String)
