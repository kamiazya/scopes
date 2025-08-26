package io.github.kamiazya.scopes.scopemanagement.application.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.currentTimestamp
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeHierarchyService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.HierarchyPolicy
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Application service that handles I/O operations for scope hierarchy.
 *
 * This service coordinates between the repository layer and the pure domain service,
 * handling all I/O operations and delegating business logic to the domain service.
 */
class ScopeHierarchyApplicationService(private val repository: ScopeRepository, private val domainService: ScopeHierarchyService) {

    companion object {
        /**
         * Maximum iterations when traversing hierarchy paths to prevent infinite loops.
         * This protects against data corruption or circular references that bypass normal validation.
         */
        private const val MAX_HIERARCHY_PATH_ITERATIONS = 1000
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
        val currentChildCount = when (val result = repository.countChildrenOf(parentId)) {
            is Either.Right -> result.value
            is Either.Left -> raise(
                ScopeHierarchyError.PersistenceFailure(
                    currentTimestamp(),
                    "countChildrenOf",
                    parentId,
                    result.value,
                ),
            )
        }

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

        for (iteration in 0 until MAX_HIERARCHY_PATH_ITERATIONS) {
            if (currentId == null) break

            path.add(currentId)

            val scopeResult = repository.findById(currentId)
            val scope = when (scopeResult) {
                is Either.Right -> scopeResult.value
                is Either.Left -> {
                    // For actual persistence failures, wrap the error
                    raise(
                        ScopeHierarchyError.PersistenceFailure(
                            currentTimestamp(),
                            "findById",
                            currentId,
                            scopeResult.value,
                        ),
                    )
                }
            }

            // If scope is null but no error, it truly doesn't exist
            ensureNotNull(scope) {
                ScopeHierarchyError.ScopeInHierarchyNotFound(
                    currentTimestamp(),
                    currentId,
                )
            }

            currentId = scope.parentId
        }

        // Check if we hit the iteration limit
        ensure(currentId == null) {
            ScopeHierarchyError.CircularPath(
                currentTimestamp(),
                scopeId,
                path,
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
            val scopeResult = repository.findById(id)
            val scope = when (scopeResult) {
                is Either.Right -> scopeResult.value
                is Either.Left -> {
                    // For actual persistence failures, wrap the error
                    raise(
                        ScopeHierarchyError.PersistenceFailure(
                            currentTimestamp(),
                            "findById",
                            id,
                            scopeResult.value,
                        ),
                    )
                }
            }

            // If scope is null but no error, it truly doesn't exist
            ensureNotNull(scope) {
                ScopeHierarchyError.ScopeInHierarchyNotFound(
                    currentTimestamp(),
                    id,
                )
            }
            ancestors.add(scope)
        }

        ancestors
    }

    /**
     * Retrieves all descendants of a scope.
     *
     * @param scopeId The scope to get descendants for
     * @return Either an error or list of all descendant scopes
     */
    suspend fun getDescendants(scopeId: ScopeId): Either<ScopeHierarchyError, List<Scope>> = either {
        when (val result = repository.findDescendantsOf(scopeId)) {
            is Either.Right -> result.value
            is Either.Left -> raise(
                ScopeHierarchyError.PersistenceFailure(
                    currentTimestamp(),
                    "findDescendantsOf",
                    scopeId,
                    result.value,
                ),
            )
        }
    }
}
