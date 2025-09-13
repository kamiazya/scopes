package io.github.kamiazya.scopes.scopemanagement.application.query.aspect

import io.github.kamiazya.scopes.scopemanagement.application.query.Query

/**
 * Query to list all aspect definitions.
 */
data class ListAspectDefinitions(val offset: Int = 0, val limit: Int = 100) : Query
