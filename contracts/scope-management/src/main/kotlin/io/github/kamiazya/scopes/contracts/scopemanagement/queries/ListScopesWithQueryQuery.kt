package io.github.kamiazya.scopes.contracts.scopemanagement.queries

/**
 * Query to list scopes filtered by an advanced aspect query.
 * Supports comparison operators (=, !=, >, >=, <, <=) and logical operators (AND, OR, NOT).
 *
 * @property aspectQuery The query string (e.g., "priority=high AND status!=closed")
 * @property parentId Optional parent ID to filter children
 * @property offset Number of items to skip for pagination
 * @property limit Maximum number of items to return
 */
public data class ListScopesWithQueryQuery(val aspectQuery: String, val parentId: String? = null, val offset: Int = 0, val limit: Int = 20)
