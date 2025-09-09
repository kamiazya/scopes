package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.platform.domain.error.currentTimestamp
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
                        occurredAt = currentTimestamp(),
                        attemptedValue = maxDepth,
                    )
                }
            }

            if (maxChildrenPerScope != null) {
                ensure(maxChildrenPerScope > 0) {
                    HierarchyPolicyError.InvalidMaxChildrenPerScope(
                        occurredAt = currentTimestamp(),
                        attemptedValue = maxChildrenPerScope,
                    )
                }
            }

            HierarchyPolicy(maxDepth, maxChildrenPerScope)
        }
    }
}
