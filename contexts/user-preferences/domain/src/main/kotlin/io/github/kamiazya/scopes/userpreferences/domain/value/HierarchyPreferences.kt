package io.github.kamiazya.scopes.userpreferences.domain.value

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError
import kotlin.ConsistentCopyVisibility

/**
 * Value object representing user's hierarchy preferences.
 *
 * This only stores the user's preferred settings without enforcing
 * business rules. The actual policy enforcement happens in the
 * Scope Management context.
 */
@ConsistentCopyVisibility
data class HierarchyPreferences private constructor(
    val maxDepth: Int? = null, // null means no preference (use system default)
    val maxChildrenPerScope: Int? = null, // null means no preference (use system default)
) {
    companion object {
        /**
         * Creates hierarchy preferences with basic validation.
         * Only checks for obviously invalid values (negative or zero).
         * Business rule validation happens in the scope-management context.
         */
        fun create(maxDepth: Int? = null, maxChildrenPerScope: Int? = null): Either<UserPreferencesError, HierarchyPreferences> {
            // Only validate that values are positive if provided
            if (maxDepth != null && maxDepth <= 0) {
                return UserPreferencesError.InvalidHierarchyPreferences(
                    "Maximum depth must be positive if specified",
                ).left()
            }
            if (maxChildrenPerScope != null && maxChildrenPerScope <= 0) {
                return UserPreferencesError.InvalidHierarchyPreferences(
                    "Maximum children per scope must be positive if specified",
                ).left()
            }
            return HierarchyPreferences(maxDepth, maxChildrenPerScope).right()
        }

        /**
         * Default preferences with no specific limits.
         */
        val DEFAULT = HierarchyPreferences(
            maxDepth = null,
            maxChildrenPerScope = null,
        )
    }
}
