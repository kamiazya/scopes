package io.github.kamiazya.scopes.contracts.scopemanagement.queries

/**
 * Query for retrieving a scope by its ID.
 *
 * This is a minimal contract for scope retrieval that contains only
 * the essential fields needed by external consumers.
 */
public data class GetScopeQuery(public val id: String)
