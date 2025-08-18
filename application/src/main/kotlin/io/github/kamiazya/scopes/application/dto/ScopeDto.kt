package io.github.kamiazya.scopes.application.dto

import kotlinx.datetime.Instant

/**
 * Pure DTO for scope data.
 * Contains only primitive types and standard library types.
 * No domain entities or value objects are exposed to maintain layer separation.
 *
 * Includes both canonical and custom aliases to provide complete scope information
 * without exposing internal ULID implementation details.
 */
data class ScopeDTO(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val canonicalAlias: String? = null,
    val customAliases: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
    val aspects: Map<String, List<String>> = emptyMap()
) : DTO

