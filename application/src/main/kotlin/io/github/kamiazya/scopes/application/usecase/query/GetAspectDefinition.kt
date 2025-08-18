package io.github.kamiazya.scopes.application.usecase.query

/**
 * Query to get a specific aspect definition by key.
 * Returns the definition if found (either user-defined or default), null otherwise.
 */
data class GetAspectDefinitionQuery(
    val key: String
) : Query

