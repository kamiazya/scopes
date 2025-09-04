package io.github.kamiazya.scopes.scopemanagement.application.query.dto

import io.github.kamiazya.scopes.scopemanagement.application.query.Query

/**
 * Query to get filtered scopes based on a context view.
 */
data class GetFilteredScopes(
    val contextKey: String? = null, // If null, use currently active context
    val limit: Int = 100,
    val offset: Int = 0,
) : Query
