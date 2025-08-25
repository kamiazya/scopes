package io.github.kamiazya.scopes.contracts.scopemanagement.results

import kotlinx.datetime.Instant

/**
 * Result of updating an existing scope.
 * Contains the updated scope information.
 */
public data class UpdateScopeResult(
    public val id: String,
    public val title: String,
    public val description: String?,
    public val parentId: String?,
    public val canonicalAlias: String,
    public val createdAt: Instant,
    public val updatedAt: Instant,
)
