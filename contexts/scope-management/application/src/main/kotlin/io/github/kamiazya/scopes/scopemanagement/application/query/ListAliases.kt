package io.github.kamiazya.scopes.scopemanagement.application.query

/**
 * Query to list all aliases for a specific scope.
 * Returns aliases sorted with canonical first, then by creation date.
 *
 * @property scopeId The ID of the scope to list aliases for
 */
data class ListAliases(val scopeId: String)
