package io.github.kamiazya.scopes.scopemanagement.application.dto

import kotlinx.datetime.Instant

/**
 * Filtered scopes result when applying a context filter.
 */
data class FilteredScopesResult(val scopes: List<ScopeResult>, val appliedContext: ContextViewResult?, val totalCount: Int, val filteredCount: Int) : DTO

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
    val updatedAt: Instant,
) : DTO

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
    val updatedAt: Instant,
) : DTO
