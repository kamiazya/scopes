package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.ErrorRecoveryCategory
import io.github.kamiazya.scopes.domain.error.RecoveryComplexity

/**
 * Domain service for error categorization and recovery assessment.
 */
class ErrorRecoveryDomainService {
    /**
     * Categorizes errors by their recoverability.
     */
    fun categorizeError(error: DomainError): ErrorRecoveryCategory {
        return when (error) {
            is DomainError.ScopeValidationError -> categorizeValidationError(error)
            is DomainError.ScopeBusinessRuleViolation -> categorizeBusinessRuleViolation(error)
            is DomainError.ScopeError -> ErrorRecoveryCategory.NON_RECOVERABLE
            is DomainError.InfrastructureError -> ErrorRecoveryCategory.NON_RECOVERABLE
        }
    }

    /**
     * Assesses if an error is recoverable.
     */
    fun isRecoverable(error: DomainError): Boolean {
        return categorizeError(error) == ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
    }

    /**
     * Determines the complexity of error recovery.
     */
    fun getRecoveryComplexity(error: DomainError): RecoveryComplexity {
        return when (error) {
            // Simple validation errors - direct suggestions possible
            is DomainError.ScopeValidationError.EmptyScopeTitle -> RecoveryComplexity.SIMPLE
            is DomainError.ScopeValidationError.ScopeTitleTooShort -> RecoveryComplexity.SIMPLE

            // Moderate validation errors - require user choices
            is DomainError.ScopeValidationError.ScopeTitleTooLong -> RecoveryComplexity.MODERATE
            is DomainError.ScopeValidationError.ScopeTitleContainsNewline -> RecoveryComplexity.MODERATE
            is DomainError.ScopeValidationError.ScopeDescriptionTooLong -> RecoveryComplexity.MODERATE
            is DomainError.ScopeValidationError.ScopeInvalidFormat -> RecoveryComplexity.COMPLEX

            // Moderate business rule violations - require user input but manageable
            is DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle -> RecoveryComplexity.MODERATE

            // Complex business rule violations - significant restructuring needed
            is DomainError.ScopeBusinessRuleViolation.ScopeMaxDepthExceeded -> RecoveryComplexity.COMPLEX
            is DomainError.ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded -> RecoveryComplexity.COMPLEX

            // All scope errors are complex by nature (data integrity issues)
            is DomainError.ScopeError -> RecoveryComplexity.COMPLEX

            // Infrastructure errors are complex - require technical intervention
            is DomainError.InfrastructureError -> RecoveryComplexity.COMPLEX
        }
    }

    // ===== PRIVATE DOMAIN CATEGORIZATION LOGIC =====

    /**
     * Categorizes validation errors.
     */
    private fun categorizeValidationError(error: DomainError.ScopeValidationError): ErrorRecoveryCategory {
        return when (error) {
            // Most validation errors are partially recoverable - can provide suggestions
            is DomainError.ScopeValidationError.EmptyScopeTitle,
            is DomainError.ScopeValidationError.ScopeTitleTooShort,
            is DomainError.ScopeValidationError.ScopeTitleTooLong,
            is DomainError.ScopeValidationError.ScopeTitleContainsNewline,
            is DomainError.ScopeValidationError.ScopeDescriptionTooLong -> ErrorRecoveryCategory.PARTIALLY_RECOVERABLE

            // Invalid format errors are typically non-recoverable (data type issues)
            is DomainError.ScopeValidationError.ScopeInvalidFormat -> ErrorRecoveryCategory.NON_RECOVERABLE
        }
    }

    /**
     * Categorizes business rule violations.
     */
    private fun categorizeBusinessRuleViolation(error: DomainError.ScopeBusinessRuleViolation): ErrorRecoveryCategory {
        return when (error) {
            // Business rule violations are partially recoverable - can suggest fixes but require user input
            is DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle,
            is DomainError.ScopeBusinessRuleViolation.ScopeMaxDepthExceeded,
            is DomainError.ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded ->
                ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
        }
    }
}
