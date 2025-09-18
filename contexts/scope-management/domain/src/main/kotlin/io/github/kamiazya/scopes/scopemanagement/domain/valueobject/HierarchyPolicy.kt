package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.scopemanagement.domain.error.HierarchyPolicyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import kotlin.ConsistentCopyVisibility

/**
 * Value object representing hierarchy configuration policy.
 *
 * This encapsulates the business rules and constraints for scope hierarchies,
 * including maximum depth and maximum children per scope. These values may
 * come from user preferences or system defaults, but are represented here
 * in domain-specific terms.
 */
@ConsistentCopyVisibility
data class HierarchyPolicy private constructor(
    val maxDepth: Int? = null, // null means unlimited
    val maxChildrenPerScope: Int? = null, // null means unlimited
) {
    /**
     * Checks if depth is unlimited.
     */
    fun isDepthUnlimited(): Boolean = maxDepth == null

    /**
     * Checks if children per scope is unlimited.
     */
    fun isChildrenPerScopeUnlimited(): Boolean = maxChildrenPerScope == null

    /**
     * Validates if a given depth is allowed by this policy.
     */
    fun isDepthAllowed(depth: Int): Boolean = maxDepth?.let { depth <= it } ?: true

    /**
     * Validates if a given number of children is allowed by this policy.
     */
    fun isChildrenCountAllowed(count: Int): Boolean = maxChildrenPerScope?.let { count <= it } ?: true

    /**
     * Returns the effective maximum depth (or a large number if unlimited).
     */
    fun effectiveMaxDepth(): Int = maxDepth ?: Int.MAX_VALUE

    /**
     * Returns the effective maximum children per scope (or a large number if unlimited).
     */
    fun effectiveMaxChildrenPerScope(): Int = maxChildrenPerScope ?: Int.MAX_VALUE

    /**
     * Validates depth against this policy.
     */
    fun validateDepth(depth: Int): Either<HierarchyPolicyError.DepthExceeded, Unit> = either {
        ensure(isDepthAllowed(depth)) {
            HierarchyPolicyError.DepthExceeded(
                currentDepth = depth,
                maxAllowed = maxDepth ?: Int.MAX_VALUE
            )
        }
    }

    /**
     * Validates children count against this policy.
     */
    fun validateChildrenCount(count: Int): Either<HierarchyPolicyError.TooManyChildren, Unit> = either {
        ensure(isChildrenCountAllowed(count)) {
            HierarchyPolicyError.TooManyChildren(
                currentCount = count,
                maxAllowed = maxChildrenPerScope ?: Int.MAX_VALUE
            )
        }
    }

    /**
     * Calculate remaining depth available.
     */
    fun remainingDepth(currentDepth: Int): Int? = maxDepth?.let { maxOf(0, it - currentDepth) }

    /**
     * Calculate remaining children slots available.
     */
    fun remainingChildrenSlots(currentCount: Int): Int? = maxChildrenPerScope?.let { maxOf(0, it - currentCount) }

    /**
     * Check if this policy is more restrictive than another policy.
     */
    fun isMoreRestrictiveThan(other: HierarchyPolicy): Boolean {
        val depthMoreRestrictive = when {
            this.maxDepth == null && other.maxDepth == null -> false
            this.maxDepth == null -> false  // unlimited is less restrictive
            other.maxDepth == null -> true   // any limit is more restrictive than unlimited
            else -> this.maxDepth < other.maxDepth
        }

        val childrenMoreRestrictive = when {
            this.maxChildrenPerScope == null && other.maxChildrenPerScope == null -> false
            this.maxChildrenPerScope == null -> false  // unlimited is less restrictive
            other.maxChildrenPerScope == null -> true   // any limit is more restrictive than unlimited
            else -> this.maxChildrenPerScope < other.maxChildrenPerScope
        }

        return depthMoreRestrictive || childrenMoreRestrictive
    }

    /**
     * Merge this policy with another, taking the more restrictive limits.
     */
    fun mergeRestrictive(other: HierarchyPolicy): HierarchyPolicy {
        val mergedMaxDepth = when {
            this.maxDepth == null && other.maxDepth == null -> null
            this.maxDepth == null -> other.maxDepth
            other.maxDepth == null -> this.maxDepth
            else -> minOf(this.maxDepth, other.maxDepth)
        }

        val mergedMaxChildren = when {
            this.maxChildrenPerScope == null && other.maxChildrenPerScope == null -> null
            this.maxChildrenPerScope == null -> other.maxChildrenPerScope
            other.maxChildrenPerScope == null -> this.maxChildrenPerScope
            else -> minOf(this.maxChildrenPerScope, other.maxChildrenPerScope)
        }

        return HierarchyPolicy(mergedMaxDepth, mergedMaxChildren)
    }

    /**
     * Create a more lenient policy by increasing limits.
     */
    fun increaseLimits(depthIncrease: Int = 0, childrenIncrease: Int = 0): HierarchyPolicy = copy(
        maxDepth = maxDepth?.let { it + depthIncrease },
        maxChildrenPerScope = maxChildrenPerScope?.let { it + childrenIncrease }
    )

    /**
     * Check if the policy has any restrictions at all.
     */
    fun hasRestrictions(): Boolean = maxDepth != null || maxChildrenPerScope != null

    companion object {
        /**
         * Default hierarchy policy with unlimited depth and children.
         */
        fun default(): HierarchyPolicy = HierarchyPolicy(
            maxDepth = null, // unlimited
            maxChildrenPerScope = null, // unlimited
        )

        /**
         * Creates a HierarchyPolicy with validation.
         * @param maxDepth Maximum depth allowed, null for unlimited
         * @param maxChildrenPerScope Maximum children per scope, null for unlimited
         */
        fun create(maxDepth: Int?, maxChildrenPerScope: Int?): Either<ScopesError, HierarchyPolicy> = either {
            // Only validate if values are provided (not null)
            if (maxDepth != null) {
                ensure(maxDepth > 0) {
                    HierarchyPolicyError.InvalidMaxDepth(
                        attemptedValue = maxDepth,
                    )
                }
            }

            if (maxChildrenPerScope != null) {
                ensure(maxChildrenPerScope > 0) {
                    HierarchyPolicyError.InvalidMaxChildrenPerScope(
                        attemptedValue = maxChildrenPerScope,
                    )
                }
            }

            HierarchyPolicy(maxDepth, maxChildrenPerScope)
        }
    }
}
