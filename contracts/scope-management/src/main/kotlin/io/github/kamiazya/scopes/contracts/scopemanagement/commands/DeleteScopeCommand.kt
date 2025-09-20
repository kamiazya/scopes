package io.github.kamiazya.scopes.contracts.scopemanagement.commands

/**
 * Command for deleting a scope.
 *
 * This is a minimal contract for scope deletion that contains only
 * the essential fields needed by external consumers.
 */
public data class DeleteScopeCommand(public val id: String, public val cascade: Boolean = false)
