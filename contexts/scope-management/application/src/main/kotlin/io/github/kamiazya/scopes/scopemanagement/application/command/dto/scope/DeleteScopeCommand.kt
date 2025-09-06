package io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope

/**
 * Command to delete a scope.
 *
 * Follows CQRS naming convention: all commands should end with 'Command' suffix.
 */
data class DeleteScopeCommand(val id: String, val cascade: Boolean = false)
