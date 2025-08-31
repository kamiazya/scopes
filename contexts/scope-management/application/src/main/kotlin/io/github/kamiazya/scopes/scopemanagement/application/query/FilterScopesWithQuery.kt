package io.github.kamiazya.scopes.scopemanagement.application.query

/**
 * Query to filter scopes using advanced aspect queries.
 *
 * @property query The aspect query string (e.g., "priority=high AND status!=closed")
 * @property parentId Optional parent ID to filter children
 * @property offset Number of items to skip for pagination
 * @property limit Maximum number of items to return
 */
data class FilterScopesWithQuery(val query: String, val parentId: String? = null, val offset: Int = 0, val limit: Int = 100)
