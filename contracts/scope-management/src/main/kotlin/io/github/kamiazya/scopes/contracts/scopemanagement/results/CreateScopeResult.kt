package io.github.kamiazya.scopes.contracts.scopemanagement.results

import kotlinx.datetime.Instant

/**
 * Result of creating a new scope.
 * Contains the ID and canonical alias information of the created scope.
 */
public data class CreateScopeResult(
    public val id: String,
    public val title: String,
    public val description: String?,
    public val parentId: String?,
    public val canonicalAlias: String,
    public val createdAt: Instant,
    public val updatedAt: Instant,
)
