package io.github.kamiazya.scopes.scopemanagement.domain.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.currentTimestamp
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Domain service for handling scope hierarchy business logic.
 *
 * This service encapsulates complex hierarchy operations that don't naturally
 * belong to a single aggregate. Following DDD principles, it maintains
 * domain logic within the domain layer while coordinating between aggregates.
 */
class ScopeHierarchyService {

    /**
     * Validates and calculates the depth of a scope hierarchy.
     * Detects circular references and ensures hierarchy integrity.
     *
     * @param scopeId The ID of the scope to calculate depth for
     * @param getScopeById Function to retrieve a scope by its ID
     * @return Either an error or the calculated depth
     */
    suspend fun calculateHierarchyDepth(scopeId: ScopeId, getScopeById: suspend (ScopeId) -> Scope?): Either<ScopeHierarchyError, Int> = either {
        val visited = mutableSetOf<ScopeId>()

        suspend fun calculateDepthRecursive(currentId: ScopeId?, depth: Int): Either<ScopeHierarchyError, Int> = either {
            when (currentId) {
                null -> depth
                else -> {
                    // Check for circular reference
                    ensure(!visited.contains(currentId)) {
                        ScopeHierarchyError.CircularPath(
                            currentTimestamp(),
                            currentId,
                            visited.toList(),
                        )
                    }
                    visited.add(currentId)

                    val scope = getScopeById(currentId)
                    ensureNotNull(scope) {
                        ScopeHierarchyError.ScopeInHierarchyNotFound(
                            currentTimestamp(),
                            currentId,
                        )
                    }

                    calculateDepthRecursive(scope.parentId, depth + 1).bind()
                }
            }
        }

        calculateDepthRecursive(scopeId, 0).bind()
    }

    /**
     * Validates parent-child relationship to prevent circular references.
     *
     * @param parentScope The potential parent scope
     * @param childScope The potential child scope
     * @param getScopeById Function to retrieve a scope by its ID
     * @return Either an error or Unit if valid
     */
    suspend fun validateParentChildRelationship(
        parentScope: Scope,
        childScope: Scope,
        getScopeById: suspend (ScopeId) -> Scope?,
    ): Either<ScopeHierarchyError, Unit> = either {
        // Check self-parenting
        ensure(parentScope.id != childScope.id) {
            ScopeHierarchyError.SelfParenting(
                currentTimestamp(),
                childScope.id,
            )
        }

        // Check if parent is already a descendant of child
        var currentParent = parentScope.parentId
        val visited = mutableSetOf<ScopeId>()

        while (currentParent != null) {
            ensure(!visited.contains(currentParent)) {
                ScopeHierarchyError.CircularPath(
                    currentTimestamp(),
                    childScope.id,
                    visited.toList(),
                )
            }
            visited.add(currentParent)

            ensure(currentParent != childScope.id) {
                ScopeHierarchyError.CircularReference(
                    currentTimestamp(),
                    childScope.id,
                    parentScope.id,
                )
            }

            val parent = getScopeById(currentParent)
            currentParent = parent?.parentId
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
