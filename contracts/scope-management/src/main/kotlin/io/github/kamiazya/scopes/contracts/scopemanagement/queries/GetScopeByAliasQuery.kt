package io.github.kamiazya.scopes.contracts.scopemanagement.queries

/**
 * Query to retrieve a scope by its alias name.
 *
 * This query allows fetching a scope using its human-readable alias
 * rather than its internal ID. Useful for CLI operations where users
 * work with aliases instead of ULIDs.
 *
 * @property aliasName The alias name (canonical or custom) to search for
 */
public data class GetScopeByAliasQuery(public val aliasName: String)
