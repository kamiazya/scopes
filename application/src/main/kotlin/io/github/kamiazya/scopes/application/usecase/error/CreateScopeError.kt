package io.github.kamiazya.scopes.application.usecase.error

import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.SaveScopeError
import io.github.kamiazya.scopes.domain.error.ExistsScopeError
import io.github.kamiazya.scopes.domain.error.CountScopeError
import io.github.kamiazya.scopes.domain.error.FindScopeError
import io.github.kamiazya.scopes.domain.error.ScopeValidationServiceError
import io.github.kamiazya.scopes.domain.error.TitleValidationError
import io.github.kamiazya.scopes.domain.error.UniquenessValidationError
import io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError
import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Errors specific to CreateScope use case.
 * Provides type-safe error handling with operation-specific repository errors.
 * Updated to use specific repository error types instead of generic RepositoryError.
 */
sealed class CreateScopeError {
    
    /**
     * Parent scope was not found or invalid parent ID format.
     */
    data object ParentNotFound : CreateScopeError()
    
    /**
     * Validation failed during scope creation.
     */
    data class ValidationFailed(
        val field: String,
        val reason: String
    ) : CreateScopeError()
    
    /**
     * Domain rules were violated during scope creation.
     */
    data class DomainRuleViolation(
        val domainError: DomainError
    ) : CreateScopeError()
    
    /**
     * Save operation failed during scope creation.
     */
    data class SaveFailure(
        val saveError: SaveScopeError
    ) : CreateScopeError()
    
    /**
     * Existence check operation failed.
     */
    data class ExistenceCheckFailure(
        val existsError: ExistsScopeError
    ) : CreateScopeError()
    
    /**
     * Count operation failed during validation.
     */
    data class CountFailure(
        val countError: CountScopeError
    ) : CreateScopeError()
    
    /**
     * Hierarchy traversal operation failed during validation.
     */
    data class HierarchyTraversalFailure(
        val findError: FindScopeError
    ) : CreateScopeError()
    
    /**
     * Maximum hierarchy depth exceeded.
     */
    data class HierarchyDepthExceeded(
        val maxDepth: Int,
        val currentDepth: Int
    ) : CreateScopeError()
    
    /**
     * Parent already has maximum number of children.
     */
    data class MaxChildrenExceeded(
        val parentId: ScopeId,
        val maxChildren: Int
    ) : CreateScopeError()

    // ===== NEW SERVICE-SPECIFIC ERROR TYPES =====

    /**
     * Title validation failed with service-specific error context.
     */
    data class TitleValidationFailed(
        val titleError: TitleValidationError
    ) : CreateScopeError()

    /**
     * Business rule violation with service-specific error context.
     */
    data class BusinessRuleViolationFailed(
        val businessRuleError: BusinessRuleServiceError
    ) : CreateScopeError()

    /**
     * Duplicate title validation failed with service-specific error context.
     */
    data class DuplicateTitleFailed(
        val uniquenessError: UniquenessValidationError
    ) : CreateScopeError()
}