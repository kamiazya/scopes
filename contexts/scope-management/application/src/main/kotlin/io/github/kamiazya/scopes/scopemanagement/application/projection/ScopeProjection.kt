package io.github.kamiazya.scopes.scopemanagement.application.projection

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Base interface for read-only projections/view models in the scope management context.
 *
 * Following CQRS principles, projections are:
 * - Optimized for specific read operations
 * - Denormalized for performance
 * - Updated asynchronously from domain events
 * - Can aggregate data from multiple bounded contexts
 */
interface ScopeProjection

/**
 * Lightweight scope summary projection for list views and navigation.
 * Optimized for displaying scope hierarchies and basic information.
 */
@Serializable
data class ScopeSummaryProjection(
    val id: String,
    val title: String,
    val canonicalAlias: String?,
    val parentId: String?,
    val childCount: Int,
    val hasAspects: Boolean,
    val isArchived: Boolean,
    val lastModified: Instant,
) : ScopeProjection

/**
 * Detailed scope projection for full scope views with rich metadata.
 * Includes all scope information, aliases, aspects, and related data.
 */
@Serializable
data class ScopeDetailProjection(
    val id: String,
    val title: String,
    val description: String?,
    val canonicalAlias: String?,
    val parentId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isArchived: Boolean,
    val aliases: List<AliasProjection>,
    val aspects: Map<String, String>,
    val childCount: Int,
    val depth: Int,
    val path: List<String>, // Path from root to this scope
) : ScopeProjection

/**
 * Alias information projection for efficient alias management.
 */
@Serializable
data class AliasProjection(val aliasName: String, val aliasType: String, val isCanonical: Boolean, val createdAt: Instant)

/**
 * Hierarchical scope tree projection for navigation and organizational views.
 * Optimized for displaying scope trees with lazy loading support.
 */
@Serializable
data class ScopeTreeProjection(
    val id: String,
    val title: String,
    val canonicalAlias: String?,
    val depth: Int,
    val childCount: Int,
    val isExpanded: Boolean = false,
    val children: List<ScopeTreeProjection> = emptyList(),
) : ScopeProjection

/**
 * Search-optimized scope projection with highlighted matches.
 * Designed for full-text search results with relevance scoring.
 */
@Serializable
data class ScopeSearchProjection(
    val id: String,
    val title: String,
    val titleHighlight: String?, // HTML-highlighted title
    val description: String?,
    val descriptionHighlight: String?, // HTML-highlighted description
    val canonicalAlias: String?,
    val matchType: SearchMatchType,
    val relevanceScore: Double,
    val path: List<String>,
) : ScopeProjection

@Serializable
enum class SearchMatchType {
    TITLE_EXACT,
    TITLE_PARTIAL,
    DESCRIPTION_MATCH,
    ALIAS_MATCH,
    ASPECT_MATCH,
}

/**
 * Activity timeline projection for scope history and audit trails.
 * Shows chronological changes and activities related to scopes.
 */
@Serializable
data class ScopeActivityProjection(
    val scopeId: String,
    val scopeTitle: String,
    val activityType: ActivityType,
    val activityDescription: String,
    val timestamp: Instant,
    val actor: String?, // User or system that performed the action
    val metadata: Map<String, String> = emptyMap(),
) : ScopeProjection

@Serializable
enum class ActivityType {
    CREATED,
    UPDATED,
    DELETED,
    ARCHIVED,
    RESTORED,
    ALIAS_ADDED,
    ALIAS_REMOVED,
    ALIAS_RENAMED,
    ASPECT_ADDED,
    ASPECT_UPDATED,
    ASPECT_REMOVED,
    MOVED,
}

/**
 * Metrics projection for dashboard and analytics views.
 * Aggregated statistics about scope usage and distribution.
 */
@Serializable
data class ScopeMetricsProjection(
    val totalScopes: Long,
    val rootScopes: Long,
    val maxDepth: Int,
    val averageChildrenPerScope: Double,
    val scopesWithAspects: Long,
    val uniqueAspectKeys: Set<String>,
    val aliasCount: Long,
    val lastUpdated: Instant,
) : ScopeProjection
