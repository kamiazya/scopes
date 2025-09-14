package io.github.kamiazya.scopes.scopemanagement.domain.service.hierarchy

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Domain service for handling scope hierarchy business logic.
 *
 * This service encapsulates complex hierarchy operations that don't naturally
 * belong to a single aggregate. Following DDD principles, it contains
 * only pure functions without I/O operations.
 *
 * All I/O operations (repository access) should be handled by the
 * application layer, which then calls these pure validation functions.
 *
 * @param maxDepthLimit Optional global limit on hierarchy depth (null means unlimited)
 * @param maxChildrenLimit Optional global limit on number of children per parent (null means unlimited)
 */
class ScopeHierarchyService(private val maxDepthLimit: Int? = null, private val maxChildrenLimit: Int? = null) {

    /**
     * Calculates the depth of a hierarchy path.
     *
     * @param hierarchyPath List of ScopeIds representing the path from child to root
     * @return The depth (length) of the hierarchy
     */
    fun calculateDepth(hierarchyPath: List<ScopeId>): Int = hierarchyPath.size

    /**
     * Detects circular references in a hierarchy path.
     *
     * @param hierarchyPath List of ScopeIds to check for cycles
     * @return Either an error if circular reference found, or Unit if valid
     */
    fun detectCircularReference(hierarchyPath: List<ScopeId>): Either<ScopeHierarchyError, Unit> = either {
        val seen = mutableSetOf<ScopeId>()

        for (id in hierarchyPath) {
            ensure(!seen.contains(id)) {
                ScopeHierarchyError.CircularDependency(
                    scopeId = id,
                    ancestorId = seen.first(),
                )
            }
            seen.add(id)
        }
    }

    /**
     * Validates parent-child relationship to prevent circular references.
     *
     * @param parentId The ID of the parent scope
     * @param childId The ID of the child scope
     * @param parentAncestorPath List of ancestor IDs of the parent (from parent to root)
     * @return Either an error or Unit if valid
     */
    fun validateParentChildRelationship(parentId: ScopeId, childId: ScopeId, parentAncestorPath: List<ScopeId>): Either<ScopeHierarchyError, Unit> = either {
        // Check self-parenting
        ensure(parentId != childId) {
            ScopeHierarchyError.CircularDependency(
                scopeId = childId,
                ancestorId = childId,
            )
        }

        // Check if child is in parent's ancestor path (would create cycle)
        ensure(!parentAncestorPath.contains(childId)) {
            ScopeHierarchyError.CircularDependency(
                scopeId = childId,
                ancestorId = parentId,
            )
        }
    }

    /**
     * Validates that a scope can have more children.
     *
     * @param parentId The ID of the parent scope
     * @param currentChildCount The current number of children
     * @param maxChildren Maximum allowed children (null means unlimited)
     * @return Either an error or Unit if valid
     */
    fun validateChildrenLimit(parentId: ScopeId, currentChildCount: Int, maxChildren: Int? = maxChildrenLimit): Either<ScopeHierarchyError, Unit> = either {
        // Use service-level limit if no specific limit provided
        val effectiveLimit = maxChildren ?: maxChildrenLimit
        // If effectiveLimit is null (unlimited), always valid
        if (effectiveLimit != null) {
            ensure(currentChildCount < effectiveLimit) {
                ScopeHierarchyError.MaxChildrenExceeded(
                    parentId = parentId,
                    currentCount = currentChildCount,
                    maxChildren = effectiveLimit,
                )
            }
        }
    }

    /**
     * Validates hierarchy depth doesn't exceed maximum.
     *
     * @param scopeId The ID of the scope being validated
     * @param currentDepth The current depth of the hierarchy (parent's depth)
     * @param maxDepth Maximum allowed depth (null means unlimited)
     * @return Either an error or Unit if valid
     */
    fun validateHierarchyDepth(scopeId: ScopeId, currentDepth: Int, maxDepth: Int? = maxDepthLimit): Either<ScopeHierarchyError, Unit> = either {
        // Use service-level limit if no specific limit provided
        val effectiveLimit = maxDepth ?: maxDepthLimit
        // If effectiveLimit is null (unlimited), always valid
        if (effectiveLimit != null) {
            val attemptedDepth = currentDepth + 1
            ensure(attemptedDepth <= effectiveLimit) {
                ScopeHierarchyError.MaxDepthExceeded(
                    scopeId = scopeId,
                    currentDepth = attemptedDepth,
                    maxDepth = effectiveLimit,
                )
            }
        }
    }
}
