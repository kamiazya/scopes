package io.github.kamiazya.scopes.infrastructure.error

import io.github.kamiazya.scopes.domain.error.DomainError
import arrow.core.NonEmptyList

/**
 * Utility for formatting domain errors into user-friendly messages.
 */
object ErrorFormattingUtils {

    /**
     * Formats a collection of errors into a user-friendly summary message.
     *
     * @param errors NonEmptyList of domain errors to format
     * @return Formatted error message string
     */
    fun formatErrorMessages(errors: NonEmptyList<DomainError>): String {
        val errorCount = errors.size
        val grouped = groupErrorsByType(errors)

        val summary = if (errorCount == 1) {
            "1 error found:"
        } else {
            "$errorCount errors found:"
        }

        val groupSummary = grouped.entries.joinToString(", ") { (type, errorList) ->
            "$type (${errorList.size})"
        }

        val detailedMessages = errors.toList().joinToString("\n") { error ->
            "- ${getErrorMessage(error)}"
        }

        return "$summary\n$groupSummary\n\n$detailedMessages"
    }

    /**
     * Groups errors by their type name for summary display.
     * This is a presentation utility, not domain logic.
     */
    private fun groupErrorsByType(errors: NonEmptyList<DomainError>): Map<String, List<DomainError>> {
        return errors.toList().groupBy { error ->
            when (error) {
                is DomainError.ScopeValidationError -> "ScopeValidationError"
                is DomainError.ScopeError -> "ScopeError"
                is DomainError.ScopeBusinessRuleViolation -> "ScopeBusinessRuleViolation"
            }
        }
    }

    /**
     * Converts a domain error to a human-readable message.
     *
     * @param error Domain error to convert
     * @return Human-readable error message
     */
    fun getErrorMessage(error: DomainError): String {
        return when (error) {
            is DomainError.ScopeValidationError -> getValidationErrorMessage(error)
            is DomainError.ScopeError -> getScopeErrorMessage(error)
            is DomainError.ScopeBusinessRuleViolation -> getBusinessRuleViolationMessage(error)
        }
    }

    /**
     * Converts a validation error to a human-readable message.
     */
    fun getValidationErrorMessage(error: DomainError.ScopeValidationError): String {
        return when (error) {
            is DomainError.ScopeValidationError.EmptyScopeTitle -> "Empty title"
            is DomainError.ScopeValidationError.ScopeTitleTooShort -> "Title too short"
            is DomainError.ScopeValidationError.ScopeTitleTooLong ->
                "Title too long (max: ${error.maxLength}, actual: ${error.actualLength})"
            is DomainError.ScopeValidationError.ScopeTitleContainsNewline ->
                "Title must not contain newline characters"
            is DomainError.ScopeValidationError.ScopeDescriptionTooLong ->
                "Description too long (max: ${error.maxLength}, actual: ${error.actualLength})"
            is DomainError.ScopeValidationError.ScopeInvalidFormat ->
                "Invalid format for ${error.field}, expected: ${error.expected}"
        }
    }

    /**
     * Converts a scope error to a human-readable message.
     */
    fun getScopeErrorMessage(error: DomainError.ScopeError): String {
        return when (error) {
            is DomainError.ScopeError.ScopeNotFound -> "Scope not found"
            is DomainError.ScopeError.SelfParenting -> "Scope cannot be its own parent"
            is DomainError.ScopeError.CircularReference ->
                "Circular reference detected between ${error.scopeId.value} and ${error.parentId.value}"
            is DomainError.ScopeError.InvalidTitle -> "Invalid title: ${error.reason}"
            is DomainError.ScopeError.InvalidDescription -> "Invalid description: ${error.reason}"
            is DomainError.ScopeError.InvalidParent ->
                "Invalid parent ${error.parentId.value}: ${error.reason}"
        }
    }

    /**
     * Converts a business rule violation to a human-readable message.
     */
    fun getBusinessRuleViolationMessage(error: DomainError.ScopeBusinessRuleViolation): String {
        return when (error) {
            is DomainError.ScopeBusinessRuleViolation.ScopeMaxDepthExceeded ->
                "Maximum hierarchy depth exceeded (max: ${error.maxDepth}, actual: ${error.actualDepth})"
            is DomainError.ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded ->
                "Maximum children exceeded (max: ${error.maxChildren}, actual: ${error.actualChildren})"
            is DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle -> "Duplicate title: ${error.title}"
        }
    }
}
