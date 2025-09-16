package io.github.kamiazya.scopes.scopemanagement.application.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.platform.observability.Loggable
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.AvailabilityReason
import io.github.kamiazya.scopes.scopemanagement.domain.error.HierarchyOperation
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.hierarchy.ScopeHierarchyService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.HierarchyPolicy
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Application service that handles I/O operations for scope hierarchy.
 *
 * This service coordinates between the repository layer and the pure domain service,
 * handling all I/O operations and delegating business logic to the domain service.
 */
class ScopeHierarchyApplicationService(private val repository: ScopeRepository, private val domainService: ScopeHierarchyService) {

    companion object : Loggable {
        /**
         * Maximum iterations when traversing hierarchy paths to prevent infinite loops.
         * This protects against data corruption or circular references that bypass normal validation.
         */
        private const val MAX_HIERARCHY_PATH_ITERATIONS = 1000

        /**
         * Maps persistence errors to domain-specific hierarchy errors.
         * Logs technical details while returning business-meaningful errors.
         */
        private fun mapPersistenceError(error: ScopesError, operation: HierarchyOperation, scopeId: ScopeId? = null): ScopeHierarchyError {
            // Log technical details for debugging
            logger.debug(
                "Hierarchy operation failed",
                mapOf(
                    "operation" to operation.name,
                    "scopeId" to (scopeId?.value ?: "null"),
                    "error" to error.toString(),
                ),
            )

            // Map to business-meaningful error
            val reason = when (error) {
                is ScopesError.ConcurrencyError -> AvailabilityReason.CONCURRENT_MODIFICATION
                is ScopesError.RepositoryError -> AvailabilityReason.TEMPORARILY_UNAVAILABLE
                else -> AvailabilityReason.TEMPORARILY_UNAVAILABLE
            }

            return ScopeHierarchyError.HierarchyUnavailable(
                scopeId = scopeId,
                operation = operation,
                reason = reason,
            )
        }
    }

    /**
     * Calculates the hierarchy depth for a scope by building its hierarchy path.
     *
     * @param scopeId The ID of the scope to calculate depth for
     * @return Either an error or the calculated depth
     */
    suspend fun calculateHierarchyDepth(scopeId: ScopeId): Either<ScopeHierarchyError, Int> = either {
        val hierarchyPath = buildHierarchyPath(scopeId).bind()

        // Detect circular references first
        domainService.detectCircularReference(hierarchyPath).bind()

        // Calculate depth using pure function
        domainService.calculateDepth(hierarchyPath)
    }

    /**
     * Validates the entire hierarchy for a scope against a policy.
     *
     * @param scopeId The ID of the scope to validate
     * @param policy The hierarchy policy to validate against
     * @return Either an error or Unit if valid
     */
    suspend fun validateHierarchy(scopeId: ScopeId, policy: HierarchyPolicy): Either<ScopeHierarchyError, Unit> = either {
        val hierarchyPath = buildHierarchyPath(scopeId).bind()

        // Check for circular references
        domainService.detectCircularReference(hierarchyPath).bind()

        // Validate depth against policy
        val depth = domainService.calculateDepth(hierarchyPath)
        domainService.validateHierarchyDepth(scopeId, depth - 1, policy.maxDepth).bind()
    }

    /**
     * Validates parent-child relationship before establishing it.
     *
     * @param parentScope The potential parent scope
     * @param childScope The potential child scope
     * @return Either an error or Unit if valid
     */
    suspend fun validateParentChildRelationship(parentScope: Scope, childScope: Scope): Either<ScopeHierarchyError, Unit> = either {
        // Build parent's ancestor path
        val parentAncestorPath = parentScope.parentId?.let {
            buildHierarchyPath(it).bind()
        } ?: emptyList()

        // Validate using pure function
        domainService.validateParentChildRelationship(
            parentScope.id,
            childScope.id,
            parentAncestorPath,
        ).bind()
    }

    /**
     * Validates if a parent can accept more children according to policy.
     *
     * @param parentId The ID of the parent scope
     * @param policy The hierarchy policy with children limits
     * @return Either an error or Unit if valid
     */
    suspend fun validateChildrenLimit(parentId: ScopeId, policy: HierarchyPolicy): Either<ScopeHierarchyError, Unit> = either {
        // Get current children count from repository
        val currentChildCount = repository.countChildrenOf(parentId)
            .mapLeft { error ->
                mapPersistenceError(error, HierarchyOperation.COUNT_CHILDREN, parentId)
            }
            .bind()

        // Validate using pure function
        domainService.validateChildrenLimit(parentId, currentChildCount, policy.maxChildrenPerScope).bind()
    }

    /**
     * Builds the hierarchy path from a scope to the root.
     *
     * @param scopeId The starting scope ID
     * @return Either an error or list of scope IDs from the given scope to root
     */
    private suspend fun buildHierarchyPath(scopeId: ScopeId): Either<ScopeHierarchyError, List<ScopeId>> = either {
        val path = mutableListOf<ScopeId>()
        var currentId: ScopeId? = scopeId

        @Suppress("UnusedPrivateProperty")
        for (iteration in 0 until MAX_HIERARCHY_PATH_ITERATIONS) {
            if (currentId == null) break

            path.add(currentId)

            val scope = repository.findById(currentId)
                .mapLeft { error ->
                    mapPersistenceError(error, HierarchyOperation.RETRIEVE_SCOPE, currentId)
                }
                .bind()

            // If scope is null but no error, it truly doesn't exist
            ensureNotNull(scope) {
                ScopeHierarchyError.HierarchyUnavailable(
                    scopeId = currentId,
                    operation = HierarchyOperation.RETRIEVE_SCOPE,
                    reason = AvailabilityReason.TEMPORARILY_UNAVAILABLE,
                )
            }

            currentId = scope.parentId
        }

        // Check if we hit the iteration limit
        ensure(currentId == null) {
            ScopeHierarchyError.CircularDependency(
                scopeId = scopeId,
                ancestorId = path.firstOrNull() ?: scopeId,
            )
        }

        path
    }

    /**
     * Retrieves all ancestors of a scope.
     *
     * @param scopeId The scope to get ancestors for
     * @return Either an error or list of ancestor scopes from parent to root
     */
    suspend fun getAncestors(scopeId: ScopeId): Either<ScopeHierarchyError, List<Scope>> = either {
        val hierarchyPath = buildHierarchyPath(scopeId).bind()
        val ancestors = mutableListOf<Scope>()

        // Skip the first element (the scope itself)
        for (id in hierarchyPath.drop(1)) {
            val scope = repository.findById(id)
                .mapLeft { error ->
                    mapPersistenceError(error, HierarchyOperation.TRAVERSE_ANCESTORS, id)
                }
                .bind()

            // If scope is null but no error, it truly doesn't exist
            ensureNotNull(scope) {
                ScopeHierarchyError.HierarchyUnavailable(
                    scopeId = id,
                    operation = HierarchyOperation.RETRIEVE_SCOPE,
                    reason = AvailabilityReason.TEMPORARILY_UNAVAILABLE,
                )
            }
            ancestors.add(scope)
        }

        ancestors
    }

    /**
     * Retrieves all descendants of a scope by traversing the hierarchy.
     *
     * @param scopeId The scope to get descendants for
     * @return Either an error or list of all descendant scopes
     */
    suspend fun getDescendants(scopeId: ScopeId): Either<ScopeHierarchyError, List<Scope>> = either {
        val descendants = mutableListOf<Scope>()
        val queue = mutableListOf(scopeId)

        // Breadth-first traversal to collect all descendants
        while (queue.isNotEmpty()) {
            val currentId = queue.removeAt(0)

            // Find all children of the current scope
            val children = repository.findByParentId(currentId, offset = 0, limit = Int.MAX_VALUE)
                .mapLeft { error ->
                    mapPersistenceError(error, HierarchyOperation.FIND_DESCENDANTS, currentId)
                }
                .bind()

            // Add children to descendants and queue for further traversal
            for (child in children) {
                descendants.add(child)
                queue.add(child.id)
            }
        }

        descendants
    }
}
