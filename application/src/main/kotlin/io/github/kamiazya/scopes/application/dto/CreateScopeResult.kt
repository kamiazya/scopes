package io.github.kamiazya.scopes.application.dto

import kotlinx.datetime.Instant

/**
 * Pure DTO for scope creation result.
 * Contains only primitive types and standard library types.
 * No domain entities or value objects are exposed to maintain layer separation.
 */
data class CreateScopeResult(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val canonicalAlias: String? = null,
    val createdAt: Instant,
    val aspects: Map<String, List<String>>
) : DTO

