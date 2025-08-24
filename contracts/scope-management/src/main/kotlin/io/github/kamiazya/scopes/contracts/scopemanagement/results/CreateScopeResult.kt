package io.github.kamiazya.scopes.contracts.scopemanagement.results

import kotlinx.datetime.Instant

/**
 * Result of creating a new scope.
 * Contains the ID and canonical alias information of the created scope.
 */
data class CreateScopeResult(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val canonicalAlias: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
