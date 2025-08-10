package io.github.kamiazya.scopes.application.error

import io.github.kamiazya.scopes.application.usecase.error.CreateScopeError
import io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError
import io.github.kamiazya.scopes.domain.error.ScopeValidationServiceError

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
            is CreateScopeError.ValidationFailed -> "Validation failed: ${error.reason} (field: ${error.field})"
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
    private fun formatTitleValidationError(error: ScopeValidationServiceError.TitleValidationError): String {
        return when (error) {
            is ScopeValidationServiceError.TitleValidationError.EmptyTitle ->
                "Title cannot be empty"
            is ScopeValidationServiceError.TitleValidationError.TooShort ->
                "Title is too short (minimum ${error.minLength} characters, got ${error.actualLength})"
            is ScopeValidationServiceError.TitleValidationError.TooLong ->
                "Title is too long (maximum ${error.maxLength} characters, got ${error.actualLength})"
            is ScopeValidationServiceError.TitleValidationError.InvalidCharacters ->
                "Title contains invalid characters: ${error.invalidChars.joinToString(", ")}"
        }
    }

    /**
     * Formats business rule errors with user-friendly messages.
     */
    private fun formatBusinessRuleError(error: BusinessRuleServiceError): String {
        return when (error) {
            is BusinessRuleServiceError.ScopeBusinessRuleError.MaxDepthExceeded ->
                "Cannot create scope: maximum hierarchy depth of ${error.maxDepth} would be exceeded (attempted depth: ${error.attemptedDepth})"
            is BusinessRuleServiceError.ScopeBusinessRuleError.MaxChildrenExceeded ->
                "Cannot create scope: parent already has the maximum number of children (${error.maxChildren})"
            is BusinessRuleServiceError.ScopeBusinessRuleError.DuplicateTitleNotAllowed ->
                "Cannot create scope: duplicate title '${error.title}' not allowed in this context (${error.conflictContext})"
            is BusinessRuleServiceError.ScopeBusinessRuleError.CheckFailed ->
                "Cannot create scope: validation check '${error.checkName}' failed - ${error.errorDetails}"
            is BusinessRuleServiceError.HierarchyBusinessRuleError.SelfParentingNotAllowed ->
                "Cannot create scope: self-parenting is not allowed"
            is BusinessRuleServiceError.HierarchyBusinessRuleError.CircularReferenceNotAllowed ->
                "Cannot create scope: circular reference detected in hierarchy"
            is BusinessRuleServiceError.HierarchyBusinessRuleError.OrphanedScopeCreationNotAllowed ->
                "Cannot create scope: ${error.reason}"
            is BusinessRuleServiceError.DataIntegrityBusinessRuleError.ConsistencyCheckFailed ->
                "Cannot create scope: data consistency check failed (${error.failedChecks.joinToString(", ")})"
            is BusinessRuleServiceError.DataIntegrityBusinessRuleError.ReferentialIntegrityViolation ->
                "Cannot create scope: referential integrity violation in ${error.referenceType}"
        }
    }

    /**
     * Formats duplicate title errors with user-friendly messages.
     */
    private fun formatDuplicateTitleError(error: ScopeValidationServiceError.UniquenessValidationError): String {
        return when (error) {
            is ScopeValidationServiceError.UniquenessValidationError.DuplicateTitle -> {
                val context = if (error.parentId != null) "under the same parent" else "at the root level"
                "Title '${error.title}' already exists $context"
            }
            is ScopeValidationServiceError.UniquenessValidationError.CheckFailed ->
                "Cannot validate title uniqueness: check '${error.checkName}' failed - ${error.errorDetails}"
        }
    }
}