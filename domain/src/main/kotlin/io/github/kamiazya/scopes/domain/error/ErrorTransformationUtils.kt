package io.github.kamiazya.scopes.domain.error

import arrow.core.NonEmptyList

/**
 * Utility functions for transforming and manipulating domain errors.
 *
 * Provides common error handling patterns
 * including error grouping, formatting, context enrichment, and deduplication.
 */
object ErrorTransformationUtils {

    /**
     * Groups errors by their type name for better organization and reporting.
     *
     * @param errors NonEmptyList of domain errors to group
     * @return Map with type names as keys and lists of errors as values
     */
    fun groupErrorsByType(errors: NonEmptyList<DomainError>): Map<String, List<DomainError>> {
        return errors.toList().groupBy { error ->
            when (error) {
                is DomainError.ValidationError -> "ValidationError"
                is DomainError.ScopeError -> "ScopeError"
                is DomainError.BusinessRuleViolation -> "BusinessRuleViolation"
            }
        }
    }

    /**
     * Groups errors by their severity level for prioritized handling.
     *
     * @param errors NonEmptyList of domain errors to group
     * @return Map with severity levels as keys and lists of errors as values
     */
    fun groupErrorsBySeverity(errors: NonEmptyList<DomainError>): Map<ErrorSeverity, List<DomainError>> {
        return errors.toList().groupBy { error ->
            when (error) {
                // ValidationErrors are typically HIGH severity as they prevent basic operations
                is DomainError.ValidationError -> ErrorSeverity.HIGH

                // ScopeErrors are HIGH severity as they indicate data integrity issues
                is DomainError.ScopeError -> ErrorSeverity.HIGH

                // BusinessRuleViolations are MEDIUM severity as they enforce business constraints
                is DomainError.BusinessRuleViolation -> ErrorSeverity.MEDIUM

            }
        }
    }

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
     * Enriches errors with additional context information.
     *
     * @param errors NonEmptyList of domain errors to enrich
     * @param context ErrorContext containing additional information
     * @return NonEmptyList of enriched errors
     */
    fun enrichErrorContext(errors: NonEmptyList<DomainError>, context: ErrorContext): NonEmptyList<EnrichedError> {
        return errors.map { error ->
            EnrichedError(originalError = error, context = context)
        }
    }

    /**
     * Removes duplicate errors while preserving order.
     *
     * @param errors NonEmptyList of domain errors to deduplicate
     * @return NonEmptyList of unique errors in their original order
     */
    fun deduplicateErrors(errors: NonEmptyList<DomainError>): NonEmptyList<DomainError> {
        val uniqueErrors = errors.toList().distinctBy { it }
        return NonEmptyList(uniqueErrors.first(), uniqueErrors.drop(1))
    }

    /**
     * Filters errors based on a predicate function.
     *
     * @param errors NonEmptyList of domain errors to filter
     * @param predicate Function to test each error
     * @return List of errors matching the predicate (may be empty)
     */
    fun filterErrors(errors: NonEmptyList<DomainError>, predicate: (DomainError) -> Boolean): List<DomainError> {
        return errors.toList().filter(predicate)
    }

    /**
     * Converts a domain error to a human-readable message.
     *
     * @param error Domain error to convert
     * @return Human-readable error message
     */
    private fun getErrorMessage(error: DomainError): String {
        return when (error) {
            // Validation errors
            is DomainError.ValidationError.EmptyTitle -> "Empty title"
            is DomainError.ValidationError.TitleTooShort -> "Title too short"
            is DomainError.ValidationError.TitleTooLong -> 
                "Title too long (max: ${error.maxLength}, actual: ${error.actualLength})"
            is DomainError.ValidationError.TitleContainsNewline -> 
                "Title must not contain newline characters"
            is DomainError.ValidationError.DescriptionTooLong -> 
                "Description too long (max: ${error.maxLength}, actual: ${error.actualLength})"
            is DomainError.ValidationError.InvalidFormat -> 
                "Invalid format for ${error.field}, expected: ${error.expected}"

            // Scope errors
            is DomainError.ScopeError.ScopeNotFound -> "Scope not found"
            is DomainError.ScopeError.SelfParenting -> "Scope cannot be its own parent"
            is DomainError.ScopeError.CircularReference -> 
                "Circular reference detected between ${error.scopeId} and ${error.parentId}"
            is DomainError.ScopeError.InvalidTitle -> "Invalid title: ${error.reason}"
            is DomainError.ScopeError.InvalidDescription -> "Invalid description: ${error.reason}"
            is DomainError.ScopeError.InvalidParent -> 
                "Invalid parent ${error.parentId}: ${error.reason}"

            // Business rule violations
            is DomainError.BusinessRuleViolation.MaxDepthExceeded -> 
                "Maximum hierarchy depth exceeded (max: ${error.maxDepth}, actual: ${error.actualDepth})"
            is DomainError.BusinessRuleViolation.MaxChildrenExceeded -> 
                "Maximum children exceeded (max: ${error.maxChildren}, actual: ${error.actualChildren})"
            is DomainError.BusinessRuleViolation.DuplicateTitle -> "Duplicate title: ${error.title}"

        }
    }
}

/**
 * Error severity classification for better error handling
 */
enum class ErrorSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Error context for enriching errors with additional information
 */
data class ErrorContext(
    val operationId: String,
    val timestamp: String,
    val userId: String? = null,
    val additionalData: Map<String, Any> = emptyMap()
)

/**
 * Enriched error wrapper that includes context information
 */
data class EnrichedError(
    val originalError: DomainError,
    val context: ErrorContext
)
