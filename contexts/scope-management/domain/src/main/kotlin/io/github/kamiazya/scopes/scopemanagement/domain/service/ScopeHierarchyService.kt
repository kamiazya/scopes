package io.github.kamiazya.scopes.scopemanagement.domain.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.currentTimestamp
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
 */
class ScopeHierarchyService {

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
                ScopeHierarchyError.CircularPath(
                    currentTimestamp(),
                    id,
                    seen.toList(),
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
            ScopeHierarchyError.SelfParenting(
                currentTimestamp(),
                childId,
            )
        }

        // Check if child is in parent's ancestor path (would create cycle)
        ensure(!parentAncestorPath.contains(childId)) {
            ScopeHierarchyError.CircularReference(
                currentTimestamp(),
                scopeId = childId,
                parentId = parentId,
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
    fun validateChildrenLimit(parentId: ScopeId, currentChildCount: Int, maxChildren: Int?): Either<ScopeHierarchyError, Unit> = either {
        // If maxChildren is null (unlimited), always valid
        if (maxChildren != null) {
            ensure(currentChildCount < maxChildren) {
                ScopeHierarchyError.MaxChildrenExceeded(
                    occurredAt = currentTimestamp(),
                    parentScopeId = parentId,
                    currentChildrenCount = currentChildCount,
                    maximumChildren = maxChildren,
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
    fun validateHierarchyDepth(scopeId: ScopeId, currentDepth: Int, maxDepth: Int?): Either<ScopeHierarchyError, Unit> = either {
        // If maxDepth is null (unlimited), always valid
        if (maxDepth != null) {
            val attemptedDepth = currentDepth + 1
            ensure(attemptedDepth <= maxDepth) {
                ScopeHierarchyError.MaxDepthExceeded(
                    occurredAt = currentTimestamp(),
                    scopeId = scopeId,
                    attemptedDepth = attemptedDepth,
                    maximumDepth = maxDepth,
                )
            }
        }
    }
}
