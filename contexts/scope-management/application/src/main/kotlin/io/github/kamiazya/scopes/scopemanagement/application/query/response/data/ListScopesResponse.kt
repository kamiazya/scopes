package io.github.kamiazya.scopes.scopemanagement.application.query.response.data

import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult

/**
 * Response data for listing multiple scopes.
 *
 * Contains a collection of scopes along with pagination information and formatting
 * options that control the presentation of the list in different output formats.
 *
 * @property scopes The list of scope results from the query
 * @property totalCount Optional total number of scopes matching the query (for pagination)
 * @property hasMore Optional flag indicating more results are available beyond this page
 * @property includeAliases When true, includes alias information in the output
 * @property includeDebug When true, includes internal identifiers and debug information
 * @property isRootScopes When true, indicates this is a list of root scopes (no parents)
 */
data class ListScopesResponse(
    val scopes: List<ScopeResult>,
    val totalCount: Long? = null,
    val hasMore: Boolean? = null,
    val includeAliases: Boolean = false,
    val includeDebug: Boolean = false,
    val isRootScopes: Boolean = false,
)
