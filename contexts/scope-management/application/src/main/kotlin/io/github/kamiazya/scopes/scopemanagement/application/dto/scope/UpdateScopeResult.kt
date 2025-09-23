package io.github.kamiazya.scopes.scopemanagement.application.dto.scope

import kotlinx.datetime.Instant

/**
 * Pure DTO for scope update result.
 * Contains only primitive types and standard library types.
 * No domain entities or value objects are exposed to maintain layer separation.
 */
data class UpdateScopeResult(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val canonicalAlias: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val aspects: Map<String, List<String>>,
)
