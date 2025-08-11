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
sealed class BusinessRuleServiceError

/**
 * Hierarchy-specific business rule errors with detailed context.
 */
sealed class HierarchyBusinessRuleError : BusinessRuleServiceError() {
    
    /**
     * Represents a self-parenting business rule violation.
     * 
     * @param scopeId The scope ID that attempted to parent itself
     * @param operation The operation that was attempted
     */
    data class SelfParenting(
        val scopeId: String,
        val operation: String
    ) : HierarchyBusinessRuleError()
    
    /**
     * Represents a circular reference business rule violation.
     * 
     * @param scopeId The scope ID where the circular reference was detected
     * @param parentId The parent ID that would create the cycle
     * @param cyclePath The detected cycle path
     */
    data class CircularReference(
        val scopeId: String,
        val parentId: String,
        val cyclePath: List<String>
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
     * @param checkType The type of consistency check that failed
     * @param expectedState The expected state according to business rules
     * @param actualState The actual state found
     * @param affectedFields The fields affected by the inconsistency
     */
    data class ConsistencyCheckFailure(
        val scopeId: String,
        val checkType: String,
        val expectedState: String,
        val actualState: String,
        val affectedFields: List<String>
    ) : DataIntegrityBusinessRuleError()
}