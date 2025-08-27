package io.github.kamiazya.scopes.scopemanagement.domain.error

import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Constraint violations related to Scope hierarchy.
 */
sealed class ScopeHierarchyError : ScopesError() {

    data class CircularReference(override val occurredAt: Instant, val scopeId: ScopeId, val parentId: ScopeId) : ScopeHierarchyError()

    data class CircularPath(override val occurredAt: Instant, val scopeId: ScopeId, val cyclePath: List<ScopeId>) : ScopeHierarchyError()

    data class MaxDepthExceeded(override val occurredAt: Instant, val scopeId: ScopeId, val attemptedDepth: Int, val maximumDepth: Int) :
        ScopeHierarchyError()

    data class MaxChildrenExceeded(override val occurredAt: Instant, val parentScopeId: ScopeId, val currentChildrenCount: Int, val maximumChildren: Int) :
        ScopeHierarchyError()

    data class SelfParenting(override val occurredAt: Instant, val scopeId: ScopeId) : ScopeHierarchyError()

    data class ParentNotFound(override val occurredAt: Instant, val scopeId: ScopeId, val parentId: ScopeId) : ScopeHierarchyError()

    data class InvalidParentId(override val occurredAt: Instant, val invalidId: String) : ScopeHierarchyError()

    data class ScopeInHierarchyNotFound(override val occurredAt: Instant, val scopeId: ScopeId) : ScopeHierarchyError()

    data class HasChildren(val scopeId: ScopeId, override val occurredAt: Instant = Clock.System.now()) : ScopeHierarchyError()

    /**
     * Represents a failure in hierarchy operations due to availability issues.
     * This models business-level failures without exposing technical details.
     */
    data class HierarchyUnavailable(
        override val occurredAt: Instant,
        val scopeId: ScopeId? = null,
        val operation: HierarchyOperation,
        val reason: AvailabilityReason,
    ) : ScopeHierarchyError()
}

/**
 * Types of hierarchy operations that can fail.
 * Represents business operations in domain language.
 */
enum class HierarchyOperation {
    TRAVERSE_ANCESTORS, // Walking up the hierarchy tree to find ancestors
    COUNT_CHILDREN, // Counting direct children of a scope
    FIND_DESCENDANTS, // Finding all descendants in the hierarchy
    VERIFY_EXISTENCE, // Verifying a scope exists in the hierarchy
    RETRIEVE_SCOPE, // Retrieving scope details from the hierarchy
}

/**
 * Reasons why a hierarchy operation might be unavailable.
 * Abstracts technical failures into business-meaningful categories.
 */
enum class AvailabilityReason {
    TEMPORARILY_UNAVAILABLE, // Temporary unavailability, retry may succeed
    CORRUPTED_HIERARCHY, // Hierarchy data integrity issues detected
    CONCURRENT_MODIFICATION, // Operation failed due to concurrent changes
}
