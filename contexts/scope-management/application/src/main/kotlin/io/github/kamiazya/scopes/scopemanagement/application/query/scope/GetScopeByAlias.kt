package io.github.kamiazya.scopes.scopemanagement.application.query.scope

import io.github.kamiazya.scopes.scopemanagement.application.query.Query

/**
 * Query to retrieve a scope by its alias name.
 *
 * @property aliasName The alias name (canonical or custom) to search for
 */
data class GetScopeByAlias(val aliasName: String) : Query
