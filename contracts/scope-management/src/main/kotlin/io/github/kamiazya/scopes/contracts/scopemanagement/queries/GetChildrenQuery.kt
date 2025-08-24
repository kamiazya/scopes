package io.github.kamiazya.scopes.contracts.scopemanagement.queries

/**
 * Query for retrieving child scopes of a parent.
 *
 * This is a minimal contract for retrieving child scopes that contains only
 * the essential fields needed by external consumers.
 */
data class GetChildrenQuery(val parentId: String)
