package io.github.kamiazya.scopes.contracts.scopemanagement.queries

/**
 * Query types for context view read operations.
 */
public object ListContextViewsQuery

public data class GetContextViewQuery(val key: String)

public object GetActiveContextQuery
