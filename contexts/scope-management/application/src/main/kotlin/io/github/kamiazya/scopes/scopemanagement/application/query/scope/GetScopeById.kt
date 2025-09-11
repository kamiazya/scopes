package io.github.kamiazya.scopes.scopemanagement.application.query.scope

import io.github.kamiazya.scopes.scopemanagement.application.query.Query

/**
 * Query to retrieve a scope by its ID.
 */
data class GetScopeById(val id: String) : Query
