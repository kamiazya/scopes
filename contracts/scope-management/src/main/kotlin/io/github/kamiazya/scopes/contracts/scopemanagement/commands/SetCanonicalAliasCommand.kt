package io.github.kamiazya.scopes.contracts.scopemanagement.commands

/**
 * Command to set the canonical alias for a scope.
 *
 * @property scopeId The ID of the scope to set the canonical alias for
 * @property aliasName The alias name to set as canonical
 */
public data class SetCanonicalAliasCommand(public val scopeId: String, public val aliasName: String)
