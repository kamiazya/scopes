package io.github.kamiazya.scopes.scopemanagement.application.error

/**
 * Errors related to scope hierarchy management.
 */
sealed class ScopeHierarchyApplicationError : ScopeManagementApplicationError() {

    data class CircularReference(val scopeId: String, val cyclePath: List<String>) : ScopeHierarchyApplicationError()

    data class MaxDepthExceeded(val scopeId: String, val attemptedDepth: Int, val maximumDepth: Int) : ScopeHierarchyApplicationError()

    data class MaxChildrenExceeded(val parentScopeId: String, val currentChildrenCount: Int, val maximumChildren: Int) : ScopeHierarchyApplicationError()

    data class SelfParenting(val scopeId: String) : ScopeHierarchyApplicationError()

    data class ParentNotFound(val scopeId: String, val parentId: String) : ScopeHierarchyApplicationError()

    data class InvalidParentId(val invalidId: String) : ScopeHierarchyApplicationError()

    data class HasChildren(val scopeId: String, val childCount: Int) : ScopeHierarchyApplicationError()
}
