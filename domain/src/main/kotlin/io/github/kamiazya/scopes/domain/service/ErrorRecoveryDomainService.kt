package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.ScopeError
import io.github.kamiazya.scopes.domain.error.ScopeValidationError
import io.github.kamiazya.scopes.domain.error.ScopeBusinessRuleViolation
import io.github.kamiazya.scopes.domain.error.DomainInfrastructureError
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
            is ScopeValidationError -> categorizeValidationError(error)
            is ScopeBusinessRuleViolation -> categorizeBusinessRuleViolation(error)
            is ScopeError -> ErrorRecoveryCategory.NON_RECOVERABLE
            is DomainInfrastructureError -> ErrorRecoveryCategory.NON_RECOVERABLE
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
            is ScopeValidationError.EmptyScopeTitle -> RecoveryComplexity.SIMPLE
            is ScopeValidationError.ScopeTitleTooShort -> RecoveryComplexity.SIMPLE

            // Moderate validation errors - require user choices
            is ScopeValidationError.ScopeTitleTooLong -> RecoveryComplexity.MODERATE
            is ScopeValidationError.ScopeTitleContainsNewline -> RecoveryComplexity.MODERATE
            is ScopeValidationError.ScopeDescriptionTooLong -> RecoveryComplexity.MODERATE
            is ScopeValidationError.ScopeInvalidFormat -> RecoveryComplexity.COMPLEX

            // Moderate business rule violations - require user input but manageable
            is ScopeBusinessRuleViolation.ScopeDuplicateTitle -> RecoveryComplexity.MODERATE

            // Complex business rule violations - significant restructuring needed
            // ScopeMaxDepthExceeded consolidated into BusinessRuleServiceError.ScopeBusinessRuleError.MaxDepthExceeded
            // is ScopeBusinessRuleViolation.ScopeMaxDepthExceeded -> RecoveryComplexity.COMPLEX
            is ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded -> RecoveryComplexity.COMPLEX

            // All scope errors are complex by nature (data integrity issues)
            is ScopeError -> RecoveryComplexity.COMPLEX

            // Infrastructure errors are complex - require technical intervention
            is DomainInfrastructureError -> RecoveryComplexity.COMPLEX
        }
    }

    // ===== PRIVATE DOMAIN CATEGORIZATION LOGIC =====

    /**
     * Categorizes validation errors.
     */
    private fun categorizeValidationError(error: ScopeValidationError): ErrorRecoveryCategory {
        return when (error) {
            // Most validation errors are partially recoverable - can provide suggestions
            is ScopeValidationError.EmptyScopeTitle,
            is ScopeValidationError.ScopeTitleTooShort,
            is ScopeValidationError.ScopeTitleTooLong,
            is ScopeValidationError.ScopeTitleContainsNewline,
            is ScopeValidationError.ScopeDescriptionTooLong -> ErrorRecoveryCategory.PARTIALLY_RECOVERABLE

            // Invalid format errors are typically non-recoverable (data type issues)
            is ScopeValidationError.ScopeInvalidFormat -> ErrorRecoveryCategory.NON_RECOVERABLE
        }
    }

    /**
     * Categorizes business rule violations.
     */
    private fun categorizeBusinessRuleViolation(error: ScopeBusinessRuleViolation): ErrorRecoveryCategory {
        return when (error) {
            // Business rule violations are partially recoverable - can suggest fixes but require user input
            is ScopeBusinessRuleViolation.ScopeDuplicateTitle,
            // ScopeMaxDepthExceeded consolidated into BusinessRuleServiceError.ScopeBusinessRuleError.MaxDepthExceeded
            // is ScopeBusinessRuleViolation.ScopeMaxDepthExceeded,
            is ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded ->
                ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
        }
    }
}
