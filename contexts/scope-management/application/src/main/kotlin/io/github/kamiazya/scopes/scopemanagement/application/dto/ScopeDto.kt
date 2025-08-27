package io.github.kamiazya.scopes.scopemanagement.application.dto

import kotlinx.datetime.Instant

/**
 * Data Transfer Object for Scope entity.
 * Contains only primitive types to maintain layer separation.
 *
 * Includes both canonical and custom aliases to provide complete scope information
 * without exposing internal ULID implementation details.
 */
data class ScopeDto(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val canonicalAlias: String? = null,
    val customAliases: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
    val aspects: Map<String, List<String>> = emptyMap(),
)
