package io.github.kamiazya.scopes.scopemanagement.application.command

/**
 * Command to set a specific alias as the canonical alias for a scope.
 * The previous canonical alias will be automatically changed to a custom alias.
 *
 * @property scopeId The ID of the scope
 * @property aliasName The name of the alias to make canonical
 */
data class SetCanonicalAlias(val scopeId: String, val aliasName: String) : Command
