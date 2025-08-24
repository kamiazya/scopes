package io.github.kamiazya.scopes.contracts.scopemanagement.results

import kotlinx.datetime.Instant

/**
 * Result containing detailed scope information.
 * This is the primary DTO for returning scope data through the contract layer.
 * Replaces the previous ScopeDto for clearer naming conventions.
 */
data class ScopeResult(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val canonicalAlias: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isArchived: Boolean = false,
    val aspects: Map<String, List<String>> = emptyMap(),
)
