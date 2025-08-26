package io.github.kamiazya.scopes.scopemanagement.application.command

/**
 * Command to set a specific alias as the canonical alias for a scope.
 * Uses an existing alias to identify the scope, then promotes another alias to canonical.
 * The previous canonical alias will be automatically changed to a custom alias.
 *
 * @property currentAlias An existing alias of the scope (used to identify the scope)
 * @property newCanonicalAlias The name of the alias to make canonical
 */
data class SetCanonicalAlias(val currentAlias: String, val newCanonicalAlias: String) : Command
