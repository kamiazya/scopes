package io.github.kamiazya.scopes.scopemanagement.application.dto.scope
import kotlinx.datetime.Instant

/**
 * Result DTO for filtered scope queries.
 *
 * This DTO encapsulates the results of applying filters (via context views or direct queries)
 * to the scope collection. It provides both the filtered results and metadata about
 * the filtering operation.
 *
 * @property scopes The list of scopes that match the filter criteria
 * @property appliedContext The context view that was applied (null if direct query)
 * @property totalCount Total number of scopes in the system before filtering
 * @property filteredCount Number of scopes that match the filter criteria
 */
data class FilteredScopesResult(val scopes: List<ScopeResult>, val appliedContext: ContextViewResult?, val totalCount: Int, val filteredCount: Int)

/**
 * Simplified scope representation for query results.
 *
 * This DTO provides a lightweight view of scope data, suitable for listing operations
 * where full scope details (like aliases) are not needed. It contains only primitive
 * types to maintain clean architecture layer separation.
 *
 * @property id Unique identifier of the scope (ULID as string)
 * @property title Human-readable title of the scope
 * @property description Optional description text
 * @property parentId ID of the parent scope (null for root scopes)
 * @property aspects Key-value metadata for classification and filtering
 * @property createdAt Timestamp when the scope was created
 * @property updatedAt Timestamp of the last modification
 */
data class ScopeResult(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val aspects: Map<String, List<String>>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * DTO representing a context view in query results.
 *
 * Context views are named, persistent filters that can be activated to automatically
 * filter scope listings. This DTO provides the complete context information including
 * its current activation status.
 *
 * @property id Unique identifier of the context view
 * @property key Unique key used to reference this context (user-friendly identifier)
 * @property name Display name for the context view
 * @property filterExpression The filter query expression (e.g., "priority=high AND status=active")
 * @property description Optional description explaining the context's purpose
 * @property isActive Whether this context is currently active for filtering
 * @property createdAt Timestamp when the context was created
 * @property updatedAt Timestamp of the last modification
 */
data class ContextViewResult(
    val id: String,
    val key: String,
    val name: String,
    val filterExpression: String,
    val description: String? = null,
    val isActive: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)
