package io.github.kamiazya.scopes.contracts.scopemanagement.results

import kotlinx.datetime.Instant

/**
 * Result containing detailed scope information.
 * This is the primary DTO for returning scope data through the contract layer.
 * Replaces the previous ScopeDto for clearer naming conventions.
 */
public data class ScopeResult(
    public val id: String,
    public val title: String,
    public val description: String?,
    public val parentId: String?,
    public val canonicalAlias: String,
    public val createdAt: Instant,
    public val updatedAt: Instant,
    public val isArchived: Boolean = false,
    public val aspects: Map<String, List<String>> = emptyMap(),
)
