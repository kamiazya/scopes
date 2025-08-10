package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Service-specific error hierarchy for business rule operations.
 * 
 * This sealed class hierarchy provides detailed context for business rule violations
 * specific to domain services that enforce business constraints, enabling type-safe 
 * error handling and precise error reporting.
 * 
 * Based on Serena MCP research insights for functional domain modeling where
 * business rule violations require specific context and recovery strategies.
 */
sealed class BusinessRuleServiceError {

    /**
     * Scope-specific business rule errors with detailed context.
     */
    sealed class ScopeBusinessRuleError : BusinessRuleServiceError() {
        
        /**
         * Represents a maximum depth exceeded business rule violation.
         * 
         * @param maxDepth The maximum allowed depth according to business rules
         * @param attemptedDepth The depth that was attempted
         * @param affectedScopeId The scope ID that would violate the rule
         */
        data class MaxDepthExceeded(
            val maxDepth: Int,
            val attemptedDepth: Int,
            val affectedScopeId: ScopeId
        ) : ScopeBusinessRuleError()
        
        /**
         * Represents a maximum children exceeded business rule violation.
         * 
         * @param maxChildren The maximum allowed number of children
         * @param currentChildren The current number of children
         * @param parentId The parent scope ID that would violate the rule
         */
        data class MaxChildrenExceeded(
            val maxChildren: Int,
            val currentChildren: Int,
            val parentId: ScopeId
        ) : ScopeBusinessRuleError()
        
        /**
         * Represents a duplicate title business rule violation.
         * 
         * @param title The title that would create a duplicate
         * @param parentId The parent scope where duplication is not allowed
         * @param conflictContext Additional context about the conflict
         */
        data class DuplicateTitleNotAllowed(
            val title: String,
            val parentId: ScopeId?,
            val conflictContext: String
        ) : ScopeBusinessRuleError()
    }

    /**
     * Hierarchy-specific business rule errors with detailed context.
     */
    sealed class HierarchyBusinessRuleError : BusinessRuleServiceError() {
        
        /**
         * Represents a self-parenting business rule violation.
         * 
         * @param scopeId The scope ID that attempted to parent itself
         */
        data class SelfParentingNotAllowed(
            val scopeId: ScopeId
        ) : HierarchyBusinessRuleError()
        
        /**
         * Represents a circular reference business rule violation.
         * 
         * @param scopeId The scope ID involved in the circular reference
         * @param parentId The parent ID that would create the circular reference
         * @param circularPath The complete path showing the circular reference
         */
        data class CircularReferenceNotAllowed(
            val scopeId: ScopeId,
            val parentId: ScopeId,
            val circularPath: List<ScopeId>
        ) : HierarchyBusinessRuleError()
        
        /**
         * Represents an orphaned scope creation business rule violation.
         * 
         * @param parentId The non-existent parent ID
         * @param reason The reason why orphaned scope creation is not allowed
         */
        data class OrphanedScopeCreationNotAllowed(
            val parentId: ScopeId,
            val reason: String
        ) : HierarchyBusinessRuleError()
    }

    /**
     * Data integrity business rule errors with detailed context.
     */
    sealed class DataIntegrityBusinessRuleError : BusinessRuleServiceError() {
        
        /**
         * Represents a consistency check failure business rule violation.
         * 
         * @param scopeId The scope ID where consistency failed
         * @param failedChecks List of specific consistency checks that failed
         */
        data class ConsistencyCheckFailed(
            val scopeId: ScopeId,
            val failedChecks: List<String>
        ) : DataIntegrityBusinessRuleError()
        
        /**
         * Represents a referential integrity business rule violation.
         * 
         * @param scopeId The scope ID with the referential integrity issue
         * @param referencedId The referenced ID that causes the violation
         * @param referenceType The type of reference that is violated
         */
        data class ReferentialIntegrityViolation(
            val scopeId: ScopeId,
            val referencedId: ScopeId,
            val referenceType: String
        ) : DataIntegrityBusinessRuleError()
    }
}