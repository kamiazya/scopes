package io.github.kamiazya.scopes.scopemanagement.application.query.scope

import io.github.kamiazya.scopes.scopemanagement.application.query.Query

/**
 * Query to list all context views.
 */
data class ListContextViews(val offset: Int = 0, val limit: Int = 100) : Query
