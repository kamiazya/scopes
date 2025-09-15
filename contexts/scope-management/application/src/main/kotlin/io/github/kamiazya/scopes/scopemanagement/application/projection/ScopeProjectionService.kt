package io.github.kamiazya.scopes.scopemanagement.application.projection

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError

/**
 * Service interface for managing scope projections and view models.
 *
 * Following CQRS principles, this service:
 * - Maintains read-optimized projections
 * - Updates projections from domain events
 * - Provides efficient queries for different view scenarios
 * - Supports eventual consistency between write and read models
 */
interface ScopeProjectionService {

    /**
     * Retrieves a lightweight summary projection for list views
     */
    suspend fun getScopeSummary(scopeId: String): Either<ScopeManagementApplicationError, ScopeSummaryProjection?>

    /**
     * Retrieves detailed scope information with all related data
     */
    suspend fun getScopeDetail(scopeId: String): Either<ScopeManagementApplicationError, ScopeDetailProjection?>

    /**
     * Builds a hierarchical tree structure for navigation
     */
    suspend fun getScopeTree(
        rootScopeId: String?,
        maxDepth: Int = 10,
        offset: Int = 0,
        limit: Int = 100,
    ): Either<ScopeManagementApplicationError, List<ScopeTreeProjection>>

    /**
     * Searches scopes with full-text search capabilities
     */
    suspend fun searchScopes(
        query: String,
        parentId: String? = null,
        offset: Int = 0,
        limit: Int = 20,
    ): Either<ScopeManagementApplicationError, List<ScopeSearchProjection>>

    /**
     * Gets activity timeline for audit and history views
     */
    suspend fun getScopeActivity(scopeId: String, offset: Int = 0, limit: Int = 50): Either<ScopeManagementApplicationError, List<ScopeActivityProjection>>

    /**
     * Gets aggregated metrics for dashboard views
     */
    suspend fun getScopeMetrics(): Either<ScopeManagementApplicationError, ScopeMetricsProjection>

    /**
     * Gets scope summaries by aspect filters (optimized query)
     */
    suspend fun getScopesByAspect(
        aspectKey: String,
        aspectValue: String,
        offset: Int = 0,
        limit: Int = 20,
    ): Either<ScopeManagementApplicationError, List<ScopeSummaryProjection>>

    /**
     * Gets recently modified scopes for "recent activity" views
     */
    suspend fun getRecentlyModifiedScopes(offset: Int = 0, limit: Int = 20): Either<ScopeManagementApplicationError, List<ScopeSummaryProjection>>

    /**
     * Refreshes a specific projection from the current domain state
     * Used for eventual consistency and error recovery
     */
    suspend fun refreshProjection(scopeId: String): Either<ScopeManagementApplicationError, Unit>

    /**
     * Refreshes all projections from the current domain state
     * Used for maintenance and full synchronization
     */
    suspend fun refreshAllProjections(): Either<ScopeManagementApplicationError, Unit>
}
