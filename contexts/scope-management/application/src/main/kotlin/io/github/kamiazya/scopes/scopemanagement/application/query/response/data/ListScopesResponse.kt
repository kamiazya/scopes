package io.github.kamiazya.scopes.scopemanagement.application.query.response.data

import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult

data class ListScopesResponse(
    val scopes: List<ScopeResult>,
    val totalCount: Long? = null,
    val hasMore: Boolean? = null,
    val includeAliases: Boolean = false,
    val includeDebug: Boolean = false,
    val isRootScopes: Boolean = false,
)
