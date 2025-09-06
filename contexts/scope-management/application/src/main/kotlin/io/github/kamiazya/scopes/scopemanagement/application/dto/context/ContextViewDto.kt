package io.github.kamiazya.scopes.scopemanagement.application.dto.context
import kotlinx.datetime.Instant

/**
 * Data Transfer Object for ContextView.
 * Represents a context view for external consumption.
 */
data class ContextViewDto(
    val id: String,
    val key: String,
    val name: String,
    val filter: String,
    val description: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
