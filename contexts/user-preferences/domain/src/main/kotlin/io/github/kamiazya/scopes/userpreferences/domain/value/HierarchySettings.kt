package io.github.kamiazya.scopes.userpreferences.domain.value

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError

data class HierarchySettings(val maxDepth: Int? = null, val maxChildrenPerScope: Int? = null) {
    companion object {
        const val MIN_DEPTH = 1
        const val MIN_CHILDREN = 1
        const val REASONABLE_MAX_DEPTH = 100
        const val REASONABLE_MAX_CHILDREN = 1000

        fun create(maxDepth: Int? = null, maxChildrenPerScope: Int? = null): Either<UserPreferencesError, HierarchySettings> {
            if (maxDepth != null && maxDepth < MIN_DEPTH) {
                return UserPreferencesError.InvalidHierarchySettings(
                    "Maximum depth must be at least $MIN_DEPTH",
                ).left()
            }
            if (maxChildrenPerScope != null && maxChildrenPerScope < MIN_CHILDREN) {
                return UserPreferencesError.InvalidHierarchySettings(
                    "Maximum children per scope must be at least $MIN_CHILDREN",
                ).left()
            }
            if (maxDepth != null && maxDepth > REASONABLE_MAX_DEPTH) {
                return UserPreferencesError.InvalidHierarchySettings(
                    "Maximum depth $maxDepth exceeds reasonable limit of $REASONABLE_MAX_DEPTH",
                ).left()
            }
            if (maxChildrenPerScope != null && maxChildrenPerScope > REASONABLE_MAX_CHILDREN) {
                return UserPreferencesError.InvalidHierarchySettings(
                    "Maximum children per scope $maxChildrenPerScope exceeds reasonable limit of $REASONABLE_MAX_CHILDREN",
                ).left()
            }
            return HierarchySettings(maxDepth, maxChildrenPerScope).right()
        }

        val DEFAULT = HierarchySettings(
            maxDepth = null,
            maxChildrenPerScope = null,
        )
    }
}
