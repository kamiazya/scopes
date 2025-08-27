package io.github.kamiazya.scopes.contracts.scopemanagement.queries

/**
 * Query to retrieve all aliases for a specific scope.
 *
 * This query returns all aliases (both canonical and custom) associated
 * with a scope, allowing CLI commands and other clients to display
 * comprehensive alias information.
 *
 * @property scopeId The ID of the scope to list aliases for
 */
public data class ListAliasesQuery(public val scopeId: String)
