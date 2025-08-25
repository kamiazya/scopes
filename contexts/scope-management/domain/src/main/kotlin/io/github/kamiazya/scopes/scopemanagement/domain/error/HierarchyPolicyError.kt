package io.github.kamiazya.scopes.scopemanagement.domain.error

import kotlinx.datetime.Instant

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
    data class InvalidMaxDepth(override val occurredAt: Instant, val attemptedValue: Int, val minimumAllowed: Int = 1) : HierarchyPolicyError()

    /**
     * Error when maximum children per scope value is invalid.
     */
    data class InvalidMaxChildrenPerScope(override val occurredAt: Instant, val attemptedValue: Int, val minimumAllowed: Int = 1) : HierarchyPolicyError()
}
