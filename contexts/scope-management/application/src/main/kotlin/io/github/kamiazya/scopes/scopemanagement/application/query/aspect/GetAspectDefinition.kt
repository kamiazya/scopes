package io.github.kamiazya.scopes.scopemanagement.application.query.aspect

import io.github.kamiazya.scopes.scopemanagement.application.query.Query

/**
 * Query to retrieve an aspect definition by its key.
 */
data class GetAspectDefinition(val key: String) : Query
