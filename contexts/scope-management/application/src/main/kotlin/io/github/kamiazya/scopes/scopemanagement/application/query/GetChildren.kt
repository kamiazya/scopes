package io.github.kamiazya.scopes.scopemanagement.application.query

/**
 * Query to get children of a scope.
 */
data class GetChildren(val parentId: String?, val offset: Int = 0, val limit: Int = 100)
