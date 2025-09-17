package io.github.kamiazya.scopes.scopemanagement.domain.error

/**
 * Errors specific to hierarchy policy validation and configuration.
 *
 * These errors represent violations of hierarchy policy constraints
 * that are enforced within the Scope Management domain.
 */
sealed class HierarchyPolicyError : ScopesError() {

    /**
     * Error when maximum depth value is invalid.
     */
    data class InvalidMaxDepth(val attemptedValue: Int) : HierarchyPolicyError()

    /**
     * Error when maximum children per scope value is invalid.
     */
    data class InvalidMaxChildrenPerScope(val attemptedValue: Int) : HierarchyPolicyError()

    /**
     * Error when hierarchy depth is exceeded.
     */
    data class DepthExceeded(val currentDepth: Int, val maxAllowed: Int) : HierarchyPolicyError()

    /**
     * Error when too many children are added to a scope.
     */
    data class TooManyChildren(val currentCount: Int, val maxAllowed: Int) : HierarchyPolicyError()
}
