package io.github.kamiazya.scopes.contracts.scopemanagement.queries

/**
 * Query for retrieving root scopes (no parent).
 */
public data class GetRootScopesQuery(
    public val offset: Int = 0,
    public val limit: Int = 100,
)

