package io.github.kamiazya.scopes.domain.error

/**
 * Scope-specific business rule errors with detailed context.
 */
sealed class ScopeBusinessRuleError : BusinessRuleServiceError() {
    
    /**
     * Represents a maximum depth exceeded business rule violation.
     * 
     * @param maxDepth The maximum allowed depth according to business rules
     * @param actualDepth The actual depth that exceeded the limit
     * @param scopeId The scope ID where the violation occurred
     * @param parentPath The path from root to the parent scope
     */
    data class MaxDepthExceeded(
        val maxDepth: Int,
        val actualDepth: Int,
        val scopeId: String,
        val parentPath: List<String>
    ) : ScopeBusinessRuleError()
    
    /**
     * Represents a maximum children exceeded business rule violation.
     * 
     * @param maxChildren The maximum allowed children according to business rules
     * @param currentChildren The current number of children before the operation
     * @param parentId The parent scope ID where the violation occurred
     * @param attemptedOperation The operation that would exceed the limit
     */
    data class MaxChildrenExceeded(
        val maxChildren: Int,
        val currentChildren: Int,
        val parentId: String,
        val attemptedOperation: String
    ) : ScopeBusinessRuleError()
    
    /**
     * Represents a scope duplication business rule violation.
     * 
     * @param duplicateTitle The title that already exists
     * @param parentId The parent scope ID where duplication occurred
     * @param existingScopeId The ID of the existing scope with the same title
     * @param normalizedTitle The normalized version of the title used for comparison
     */
    data class DuplicateScope(
        val duplicateTitle: String,
        val parentId: String?,
        val existingScopeId: String,
        val normalizedTitle: String
    ) : ScopeBusinessRuleError()
}