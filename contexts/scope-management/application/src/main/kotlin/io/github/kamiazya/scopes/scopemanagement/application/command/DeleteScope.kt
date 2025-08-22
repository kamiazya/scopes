package io.github.kamiazya.scopes.scopemanagement.application.command

/**
 * Command to delete a scope.
 */
data class DeleteScope(val id: String, val cascade: Boolean = false)
