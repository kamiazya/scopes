package io.github.kamiazya.scopes.scopemanagement.application.query.scope

import io.github.kamiazya.scopes.scopemanagement.application.query.Query

/**
 * Query to retrieve a context view by its key.
 */
data class GetContextView(val key: String) : Query
