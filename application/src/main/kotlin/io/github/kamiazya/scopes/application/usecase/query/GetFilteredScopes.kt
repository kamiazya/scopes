package io.github.kamiazya.scopes.application.usecase.query

/**
 * Query to get scopes filtered by a context view.
 */
data class GetFilteredScopes(
    val contextName: String? = null,  // If null, use the active context
    val limit: Int = 100,
    val offset: Int = 0
) : Query