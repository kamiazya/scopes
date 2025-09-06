package io.github.kamiazya.scopes.scopemanagement.application.query.dto

import io.github.kamiazya.scopes.scopemanagement.application.query.Query

/**
 * Query to get root scopes (scopes without parent).
 */
data class GetRootScopes(val offset: Int = 0, val limit: Int = 100) : Query
