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
sealed class ScopeValidationServiceError {

    /**
     * Title validation specific errors with detailed context.
     */
    sealed class TitleValidationError : ScopeValidationServiceError() {
        
        /**
         * Represents an empty title validation error.
         */
        object EmptyTitle : TitleValidationError()
        
        /**
         * Represents a title that is too short.
         * 
         * @param minLength The minimum required length
         * @param actualLength The actual length provided
         */
        data class TooShort(
            val minLength: Int,
            val actualLength: Int
        ) : TitleValidationError()
        
        /**
         * Represents a title that is too long.
         * 
         * @param maxLength The maximum allowed length
         * @param actualLength The actual length provided
         */
        data class TooLong(
            val maxLength: Int,
            val actualLength: Int
        ) : TitleValidationError()
        
        /**
         * Represents a title containing invalid characters.
         * 
         * @param invalidChars The set of invalid characters found
         */
        data class InvalidCharacters(
            val invalidChars: Set<Char>
        ) : TitleValidationError()
    }

    /**
     * Description validation specific errors with detailed context.
     */
    sealed class DescriptionValidationError : ScopeValidationServiceError() {
        
        /**
         * Represents a description that is too long.
         * 
         * @param maxLength The maximum allowed length
         * @param actualLength The actual length provided
         */
        data class TooLong(
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
         * @param currentDepth The current depth that would be exceeded
         * @param parentId The ID of the parent scope
         */
        data class DepthExceeded(
            val maxDepth: Int,
            val currentDepth: Int,
            val parentId: ScopeId
        ) : HierarchyValidationError()
        
        /**
         * Represents a children limit exceeded error.
         * 
         * @param maxChildren The maximum allowed number of children
         * @param currentChildren The current number of children
         * @param parentId The ID of the parent scope
         */
        data class ChildrenLimitExceeded(
            val maxChildren: Int,
            val currentChildren: Int,
            val parentId: ScopeId
        ) : HierarchyValidationError()
        
        /**
         * Represents a circular reference error.
         * 
         * @param scopeId The ID of the scope causing the circular reference
         * @param parentId The ID of the proposed parent that would create the circle
         */
        data class CircularReference(
            val scopeId: ScopeId,
            val parentId: ScopeId
        ) : HierarchyValidationError()
    }

    /**
     * Uniqueness validation specific errors with detailed context.
     */
    sealed class UniquenessValidationError : ScopeValidationServiceError() {
        
        /**
         * Represents a duplicate title error.
         * 
         * @param title The original title that conflicts
         * @param parentId The parent scope ID where the conflict occurs
         * @param normalizedTitle The normalized version of the title used for comparison
         */
        data class DuplicateTitle(
            val title: String,
            val parentId: ScopeId?,
            val normalizedTitle: String
        ) : UniquenessValidationError()
        
        /**
         * Represents a uniqueness check failure due to repository/infrastructure issues.
         * This distinguishes repository failures from actual duplicate title violations.
         * 
         * @param checkName The name of the uniqueness check that failed to execute
         * @param errorDetails Descriptive details about the failure
         * @param title The title that was being checked when the failure occurred
         * @param parentId The parent scope ID context for the check
         * @param cause The underlying throwable that caused the check to fail
         */
        data class CheckFailed(
            val checkName: String,
            val errorDetails: String,
            val title: String,
            val parentId: ScopeId?,
            val cause: Throwable
        ) : UniquenessValidationError()
    }
}