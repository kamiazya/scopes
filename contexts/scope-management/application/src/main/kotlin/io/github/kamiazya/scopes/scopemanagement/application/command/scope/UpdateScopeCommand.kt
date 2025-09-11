package io.github.kamiazya.scopes.scopemanagement.application.command.scope

/**
 * Command to update an existing scope.
 * Named with 'Command' suffix to follow CQRS naming conventions.
 */
data class UpdateScopeCommand(val id: String, val title: String? = null, val description: String? = null, val metadata: Map<String, String> = emptyMap())
