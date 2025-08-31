package io.github.kamiazya.scopes.contracts.scopemanagement.results

/**
 * Paginated result containing a list of scopes with total count and window info.
 */
public data class ScopeListResult(public val scopes: List<ScopeResult>, public val totalCount: Int, public val offset: Int, public val limit: Int)
