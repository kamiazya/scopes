package io.github.kamiazya.scopes.scopemanagement.application.command

/**
 * Command to update an existing scope.
 */
data class UpdateScope(val id: String, val title: String? = null, val description: String? = null, val metadata: Map<String, String> = emptyMap())
