package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Service-specific error hierarchy for scope validation operations.
 * 
 * This sealed class hierarchy provides detailed context for validation errors
 * specific to the ScopeValidationService, enabling type-safe error handling
 * and precise error reporting.
 * 
 * Based on Serena MCP research insights for functional domain modeling with
 * sealed classes providing exhaustive pattern matching capabilities.
 */
sealed class ScopeValidationServiceError

/**
 * Description validation specific errors with detailed context.
 */
sealed class DescriptionValidationError : ScopeValidationServiceError() {
    
    /**
     * Represents a description that is too long.
     * 
     * @param maxLength The maximum allowed length
     * @param actualLength The actual length of the description
     */
    data class DescriptionTooLong(
        val maxLength: Int,
        val actualLength: Int
    ) : DescriptionValidationError()
}

/**
 * Hierarchy validation specific errors with detailed context.
 */
sealed class HierarchyValidationError : ScopeValidationServiceError() {
    
    /**
     * Represents a depth limit exceeded error.
     * 
     * @param maxDepth The maximum allowed depth
     * @param actualDepth The actual depth that exceeded the limit
     * @param scopeId The scope ID where the violation occurred
     */
    data class DepthLimitExceeded(
        val maxDepth: Int,
        val actualDepth: Int,
        val scopeId: ScopeId
    ) : HierarchyValidationError()
    
    /**
     * Represents an invalid parent reference error.
     * 
     * @param scopeId The scope ID with invalid parent
     * @param parentId The invalid parent ID
     * @param reason The reason why the parent is invalid
     */
    data class InvalidParentReference(
        val scopeId: ScopeId,
        val parentId: ScopeId,
        val reason: String
    ) : HierarchyValidationError()
    
    /**
     * Represents a circular hierarchy detection error.
     * 
     * @param scopeId The scope ID where the circular reference was detected
     * @param parentId The parent ID that would create the cycle
     * @param cyclePath The detected cycle path
     */
    data class CircularHierarchy(
        val scopeId: ScopeId,
        val parentId: ScopeId,
        val cyclePath: List<ScopeId>
    ) : HierarchyValidationError()
}
