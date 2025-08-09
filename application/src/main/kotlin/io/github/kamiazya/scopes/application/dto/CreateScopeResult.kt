package io.github.kamiazya.scopes.application.dto

import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import kotlinx.datetime.Instant

/**
 * Result DTO for scope creation.
 * Contains only the data needed by presentation layer, no domain entities.
 */
data class CreateScopeResult(
    val id: ScopeId,
    val title: String,
    val description: String?,
    val parentId: ScopeId?,
    val createdAt: Instant,
    val metadata: Map<String, String>
)
