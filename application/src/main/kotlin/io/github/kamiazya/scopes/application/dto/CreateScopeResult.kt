package io.github.kamiazya.scopes.application.dto

import kotlinx.datetime.Instant

/**
 * Result DTO for scope creation.
 * Contains only the data needed by presentation layer, no domain entities.
 */
data class CreateScopeResult(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val createdAt: Instant,
    val metadata: Map<String, String>
)
