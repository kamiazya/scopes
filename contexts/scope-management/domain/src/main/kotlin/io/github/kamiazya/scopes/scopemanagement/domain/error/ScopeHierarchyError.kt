package io.github.kamiazya.scopes.scopemanagement.domain.error

import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Constraint violations related to Scope hierarchy.
 */
sealed class ScopeHierarchyError : ScopesError() {

    data class CircularReference(val scopeId: ScopeId, val parentId: ScopeId) : ScopeHierarchyError()

    data class CircularPath(val scopeId: ScopeId, val cyclePath: List<ScopeId>) : ScopeHierarchyError()

    data class MaxDepthExceeded(val scopeId: ScopeId, val attemptedDepth: Int, val maximumDepth: Int) : ScopeHierarchyError()

    data class MaxChildrenExceeded(val parentScopeId: ScopeId, val currentChildrenCount: Int, val maximumChildren: Int) : ScopeHierarchyError()

    data class SelfParenting(val scopeId: ScopeId) : ScopeHierarchyError()

    data class ParentNotFound(val scopeId: ScopeId, val parentId: ScopeId) : ScopeHierarchyError()

    data class InvalidParentId(val invalidId: String) : ScopeHierarchyError()

    data class ScopeInHierarchyNotFound(val scopeId: ScopeId) : ScopeHierarchyError()

    data class HasChildren(val scopeId: ScopeId) : ScopeHierarchyError()

    /**
     * Represents a failure in hierarchy operations due to availability issues.
     * This models business-level failures without exposing technical details.
     */
    data class HierarchyUnavailable(val scopeId: ScopeId? = null, val operation: HierarchyOperation, val reason: AvailabilityReason) : ScopeHierarchyError()
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
