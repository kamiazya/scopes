package io.github.kamiazya.scopes.application.dto

import kotlinx.datetime.Instant

/**
 * DTOs for context view operations.
 * These DTOs prevent domain entities from leaking to presentation layer.
 */

/**
 * Result DTO for context view operations.
 */
data class ContextViewResult(
    val id: String,
    val key: String,
    val name: String,
    val filterExpression: String,
    val description: String? = null,
    val isActive: Boolean = false, // Whether this context is currently active
    val createdAt: Instant,
    val updatedAt: Instant
) : DTO

/**
 * List DTO containing multiple context views.
 */
data class ContextViewListResult(
    val contexts: List<ContextViewResult>,
    val activeContext: ContextViewResult? = null
) : DTO

/**
 * Filtered scopes result when applying a context filter.
 */
data class FilteredScopesResult(
    val scopes: List<ScopeResult>,
    val appliedContext: ContextViewResult?,
    val totalCount: Int,
    val filteredCount: Int
) : DTO

/**
 * Simple scope result for filtered views.
 */
data class ScopeResult(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val aspects: Map<String, List<String>>,
    val createdAt: Instant,
    val updatedAt: Instant
) : DTO

