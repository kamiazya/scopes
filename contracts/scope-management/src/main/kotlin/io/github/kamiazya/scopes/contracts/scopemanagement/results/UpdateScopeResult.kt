package io.github.kamiazya.scopes.contracts.scopemanagement.results

import kotlinx.datetime.Instant

/**
 * Result of updating an existing scope.
 * Contains the updated scope information.
 */
data class UpdateScopeResult(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val canonicalAlias: String,
    val updatedAt: Instant,
)
