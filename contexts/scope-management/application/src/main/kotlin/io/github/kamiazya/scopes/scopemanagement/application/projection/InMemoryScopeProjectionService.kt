package io.github.kamiazya.scopes.scopemanagement.application.projection

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * In-memory implementation of ScopeProjectionService for development and testing.
 *
 * This implementation provides:
 * - Fast in-memory lookups for development
 * - Thread-safe operations using ConcurrentHashMap
 * - Basic projection management without persistence
 * - Foundation for future database-backed implementations
 *
 * Note: In production, this would be replaced with a persistent implementation
 * using event sourcing and dedicated read model storage.
 */
class InMemoryScopeProjectionService : ScopeProjectionService {

    private val mutex = Mutex()
    private var scopeSummaries = mutableMapOf<String, ScopeSummaryProjection>()
    private var scopeDetails = mutableMapOf<String, ScopeDetailProjection>()
    private var scopeActivities = mutableMapOf<String, MutableList<ScopeActivityProjection>>()
    private var cachedMetrics: ScopeMetricsProjection? = null

    override suspend fun getScopeSummary(scopeId: String): Either<ScopeManagementApplicationError, ScopeSummaryProjection?> = scopeSummaries[scopeId].right()

    override suspend fun getScopeDetail(scopeId: String): Either<ScopeManagementApplicationError, ScopeDetailProjection?> = scopeDetails[scopeId].right()

    override suspend fun getScopeTree(
        rootScopeId: String?,
        maxDepth: Int,
        offset: Int,
        limit: Int,
    ): Either<ScopeManagementApplicationError, List<ScopeTreeProjection>> {
        val rootScopes = if (rootScopeId != null) {
            scopeDetails[rootScopeId]?.let { listOf(it) } ?: emptyList()
        } else {
            scopeDetails.values.filter { it.parentId == null }
        }

        val treeNodes = rootScopes
            .drop(offset)
            .take(limit)
            .map { buildTreeNode(it, 0, maxDepth) }
        return treeNodes.right()
    }

    private fun buildTreeNode(scope: ScopeDetailProjection, currentDepth: Int, maxDepth: Int): ScopeTreeProjection {
        val children = if (currentDepth < maxDepth) {
            scopeDetails.values
                .filter { it.parentId == scope.id }
                .map { buildTreeNode(it, currentDepth + 1, maxDepth) }
        } else {
            emptyList()
        }

        return ScopeTreeProjection(
            id = scope.id,
            title = scope.title,
            canonicalAlias = scope.canonicalAlias,
            depth = currentDepth,
            childCount = scope.childCount,
            isExpanded = false,
            children = children,
        )
    }

    override suspend fun searchScopes(
        query: String,
        parentId: String?,
        offset: Int,
        limit: Int,
    ): Either<ScopeManagementApplicationError, List<ScopeSearchProjection>> {
        val searchResults = scopeDetails.values
            .filter { scope ->
                val matchesParent = parentId == null || isDescendantOf(scope.id, parentId)
                val matchesQuery = scope.title.contains(query, ignoreCase = true) ||
                    scope.description?.contains(query, ignoreCase = true) == true ||
                    scope.aliases.any { it.aliasName.contains(query, ignoreCase = true) }
                matchesParent && matchesQuery
            }
            .drop(offset)
            .take(limit)
            .map { scope ->
                val matchType = when {
                    scope.title.equals(query, ignoreCase = true) -> SearchMatchType.TITLE_EXACT
                    scope.title.contains(query, ignoreCase = true) -> SearchMatchType.TITLE_PARTIAL
                    scope.description?.contains(query, ignoreCase = true) == true -> SearchMatchType.DESCRIPTION_MATCH
                    scope.aliases.any { it.aliasName.contains(query, ignoreCase = true) } -> SearchMatchType.ALIAS_MATCH
                    else -> SearchMatchType.ASPECT_MATCH
                }

                ScopeSearchProjection(
                    id = scope.id,
                    title = scope.title,
                    titleHighlight = highlightMatch(scope.title, query),
                    description = scope.description,
                    descriptionHighlight = scope.description?.let { highlightMatch(it, query) },
                    canonicalAlias = scope.canonicalAlias,
                    matchType = matchType,
                    relevanceScore = calculateRelevanceScore(scope, query),
                    path = scope.path,
                )
            }
            .sortedByDescending { it.relevanceScore }

        return searchResults.right()
    }

    private fun isDescendantOf(scopeId: String, ancestorId: String): Boolean {
        var currentScope = scopeDetails[scopeId] ?: return false
        while (currentScope.parentId != null) {
            if (currentScope.parentId == ancestorId) return true
            currentScope = scopeDetails[currentScope.parentId!!] ?: break
        }
        return false
    }

    private fun highlightMatch(text: String, query: String): String = text.replace(query, "<mark>$query</mark>", ignoreCase = true)

    private fun calculateRelevanceScore(scope: ScopeDetailProjection, query: String): Double {
        var score = 0.0

        // Title matches have highest priority
        if (scope.title == query) {
            score += 100.0
        } else if (scope.title.contains(query, ignoreCase = true)) {
            score += 50.0
        }

        // Alias matches
        scope.aliases.forEach { alias ->
            if (alias.aliasName == query) {
                score += 80.0
            } else if (alias.aliasName.contains(query, ignoreCase = true)) {
                score += 40.0
            }
        }

        // Description matches
        scope.description?.let { desc ->
            if (desc.contains(query, ignoreCase = true)) score += 25.0
        }

        // Boost score for shorter scopes (more specific)
        score *= (100.0 / (scope.title.length + 1))

        return score
    }

    override suspend fun getScopeActivity(scopeId: String, offset: Int, limit: Int): Either<ScopeManagementApplicationError, List<ScopeActivityProjection>> {
        val activities = scopeActivities[scopeId]
            ?.drop(offset)
            ?.take(limit)
            ?: emptyList()

        return activities.right()
    }

    override suspend fun getScopeMetrics(): Either<ScopeManagementApplicationError, ScopeMetricsProjection> {
        val metrics = cachedMetrics ?: calculateMetrics()
        cachedMetrics = metrics
        return metrics.right()
    }

    private fun calculateMetrics(): ScopeMetricsProjection {
        val scopes = scopeDetails.values

        return ScopeMetricsProjection(
            totalScopes = scopes.size.toLong(),
            rootScopes = scopes.count { it.parentId == null }.toLong(),
            maxDepth = scopes.maxOfOrNull { it.depth } ?: 0,
            averageChildrenPerScope = scopes.map { it.childCount }.average(),
            scopesWithAspects = scopes.count { it.aspects.isNotEmpty() }.toLong(),
            uniqueAspectKeys = scopes.flatMap { it.aspects.keys }.toSet(),
            aliasCount = scopes.sumOf { it.aliases.size }.toLong(),
            lastUpdated = Clock.System.now(),
        )
    }

    override suspend fun getScopesByAspect(
        aspectKey: String,
        aspectValue: String,
        offset: Int,
        limit: Int,
    ): Either<ScopeManagementApplicationError, List<ScopeSummaryProjection>> {
        val matchingScopes = scopeDetails.values
            .filter { it.aspects[aspectKey] == aspectValue }
            .drop(offset)
            .take(limit)
            .mapNotNull { scopeSummaries[it.id] }

        return matchingScopes.right()
    }

    override suspend fun getRecentlyModifiedScopes(offset: Int, limit: Int): Either<ScopeManagementApplicationError, List<ScopeSummaryProjection>> {
        val recentScopes = scopeSummaries.values
            .sortedByDescending { it.lastModified }
            .drop(offset)
            .take(limit)

        return recentScopes.right()
    }

    override suspend fun refreshProjection(scopeId: String): Either<ScopeManagementApplicationError, Unit> {
        // In a real implementation, this would rebuild the projection from domain events
        // For now, we just verify the scope exists
        return if (scopeDetails.containsKey(scopeId)) {
            Unit.right()
        } else {
            // Create ScopeId from string for the error
            ScopeId.create(scopeId).fold(
                { it.toGenericApplicationError().left() },
                { _ ->
                    ScopeManagementApplicationError.PersistenceError.NotFound(
                        entityType = "Scope",
                        entityId = scopeId,
                    ).left()
                },
            )
        }
    }

    override suspend fun refreshAllProjections(): Either<ScopeManagementApplicationError, Unit> {
        // In a real implementation, this would rebuild all projections from domain events
        cachedMetrics = null // Invalidate cached metrics
        return Unit.right()
    }

    // Internal methods for managing projections (would be called by event handlers)

    suspend fun addScopeProjection(summary: ScopeSummaryProjection, detail: ScopeDetailProjection) = mutex.withLock {
        scopeSummaries[summary.id] = summary
        scopeDetails[detail.id] = detail
        cachedMetrics = null // Invalidate cached metrics
    }

    suspend fun removeScopeProjection(scopeId: String) = mutex.withLock {
        scopeSummaries.remove(scopeId)
        scopeDetails.remove(scopeId)
        scopeActivities.remove(scopeId)
        cachedMetrics = null // Invalidate cached metrics
    }

    suspend fun addScopeActivity(activity: ScopeActivityProjection) = mutex.withLock {
        scopeActivities.computeIfAbsent(activity.scopeId) { mutableListOf() }
            .add(activity)
    }
}
