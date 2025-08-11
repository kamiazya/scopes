package io.github.kamiazya.scopes.application.error

import io.github.kamiazya.scopes.application.usecase.error.CreateScopeError
import io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError
import io.github.kamiazya.scopes.domain.error.ScopeValidationServiceError
import io.github.kamiazya.scopes.domain.error.TitleValidationError
import io.github.kamiazya.scopes.domain.error.ScopeBusinessRuleError
import io.github.kamiazya.scopes.domain.error.HierarchyBusinessRuleError
import io.github.kamiazya.scopes.domain.error.DataIntegrityBusinessRuleError
import io.github.kamiazya.scopes.domain.error.UniquenessValidationError

/**
 * Centralized translator for CreateScopeError instances to user-friendly messages.
 * 
 * This class provides a single source of truth for error message formatting,
 * keeping presentation layer code clean and ensuring consistent messaging across
 * different presentation interfaces (CLI, web, API, etc.).
 */
class CreateScopeErrorMessageTranslator {

    /**
     * Translates a CreateScopeError to a user-friendly message.
     */
    fun toUserMessage(error: CreateScopeError): String {
        return when (error) {
            is CreateScopeError.ParentNotFound -> "Parent scope not found"
            is CreateScopeError.ValidationFailed -> {
                // Sanitize error messages to avoid exposing internal details
                val sanitizedReason = when {
                    error.reason.contains("Cross-aggregate uniqueness check failed") -> "Title must be unique within the scope"
                    error.reason.contains("Failed to verify parent scope") -> "Unable to verify parent scope"
                    error.reason.contains("Aggregate consistency validation failed") -> "System validation failed"
                    error.reason.contains("Query timeout") || error.reason.contains("timeout") -> "Operation timed out"
                    error.reason.contains("Connection failure") || error.reason.contains("connection") -> "Service temporarily unavailable" 
                    error.reason.contains("Index corruption") || error.reason.contains("corruption") -> "System validation error"
                    error.reason.contains("Persistence error") || error.reason.contains("persistence") -> "Unable to complete operation"
                    error.reason.contains("errorCode") || error.reason.contains("repository") -> "System validation error"
                    else -> error.reason
                }
                "Validation failed: $sanitizedReason (field: ${error.field})"
            }
            is CreateScopeError.DomainRuleViolation -> "Domain rule violated: ${error.domainError}"
            is CreateScopeError.SaveFailure -> "Save operation failed: ${error.saveError}"
            is CreateScopeError.ExistenceCheckFailure -> "Existence check failed: ${error.existsError}"
            is CreateScopeError.CountFailure -> "Count operation failed: ${error.countError}"
            is CreateScopeError.HierarchyTraversalFailure -> "Hierarchy traversal failed: ${error.findError}"
            is CreateScopeError.HierarchyDepthExceeded -> "Maximum hierarchy depth (${error.maxDepth}) exceeded"
            is CreateScopeError.MaxChildrenExceeded -> "Parent ${error.parentId} already has maximum children (${error.maxChildren})"
            
            // Service-specific error types with improved user messages
            is CreateScopeError.TitleValidationFailed -> formatTitleValidationError(error.titleError)
            is CreateScopeError.BusinessRuleViolationFailed -> formatBusinessRuleError(error.businessRuleError)
            is CreateScopeError.DuplicateTitleFailed -> formatDuplicateTitleError(error.uniquenessError)
        }
    }

    /**
     * Formats title validation errors with user-friendly messages.
     */
    private fun formatTitleValidationError(error: TitleValidationError): String {
        return when (error) {
            is TitleValidationError.EmptyTitle ->
                "Title cannot be empty"
            is TitleValidationError.TitleTooShort ->
                "Title is too short (minimum ${error.minLength} characters, got ${error.actualLength})"
            is TitleValidationError.TitleTooLong ->
                "Title is too long (maximum ${error.maxLength} characters, got ${error.actualLength})"
            is TitleValidationError.InvalidCharacters ->
                "Title contains invalid characters: ${error.invalidCharacters.joinToString(", ")}"
        }
    }

    /**
     * Formats business rule errors with user-friendly messages.
     */
    private fun formatBusinessRuleError(error: BusinessRuleServiceError): String {
        return when (error) {
            is ScopeBusinessRuleError.MaxDepthExceeded ->
                "Cannot create scope: maximum hierarchy depth of ${error.maxDepth} would be exceeded (attempted depth: ${error.actualDepth})"
            is ScopeBusinessRuleError.MaxChildrenExceeded ->
                "Cannot create scope: parent already has the maximum number of children (${error.maxChildren})"
            is ScopeBusinessRuleError.DuplicateScope ->
                "Cannot create scope: duplicate title '${error.duplicateTitle}' not allowed in this context"
            is HierarchyBusinessRuleError.SelfParenting ->
                "Cannot create scope: self-parenting is not allowed"
            is HierarchyBusinessRuleError.CircularReference ->
                "Cannot create scope: circular reference detected in hierarchy"
            is DataIntegrityBusinessRuleError.ConsistencyCheckFailure ->
                "Cannot create scope: data consistency check failed (${error.checkType})"
        }
    }

    /**
     * Formats duplicate title errors with user-friendly messages.
     */
    private fun formatDuplicateTitleError(error: UniquenessValidationError): String {
        return when (error) {
            is UniquenessValidationError.DuplicateTitle -> {
                val context = if (error.parentId != null) "under the same parent" else "at the root level"
                "Title '${error.title}' already exists $context"
            }
        }
    }
}