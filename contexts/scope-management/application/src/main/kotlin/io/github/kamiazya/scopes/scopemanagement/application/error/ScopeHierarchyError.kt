package io.github.kamiazya.scopes.scopemanagement.application.error

/**
 * Errors related to scope hierarchy management.
 */
sealed class ScopeHierarchyError(recoverable: Boolean = true) : ApplicationError(recoverable) {
    data class CircularReference(val scopeId: String, val cyclePath: List<String>) : ScopeHierarchyError(false)

    data class MaxDepthExceeded(val scopeId: String, val attemptedDepth: Int, val maximumDepth: Int) : ScopeHierarchyError()

    data class MaxChildrenExceeded(val parentScopeId: String, val currentChildrenCount: Int, val maximumChildren: Int) : ScopeHierarchyError()

    data class SelfParenting(val scopeId: String) : ScopeHierarchyError()

    data class ParentNotFound(val scopeId: String, val parentId: String) : ScopeHierarchyError()

    data class InvalidParentId(val invalidId: String) : ScopeHierarchyError()
}
