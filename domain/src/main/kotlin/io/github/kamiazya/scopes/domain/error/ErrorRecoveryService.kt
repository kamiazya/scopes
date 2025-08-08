package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Error Recovery Service
 *
 * Service that provides helpful suggestions for common validation failures.
 * All suggestions require explicit user consent - no automatic modifications are made.
 * Follows functional programming principles with pure functions and immutable data.
 */
class ErrorRecoveryService(
    private val configuration: ScopeRecoveryConfiguration.Complete = ScopeRecoveryConfiguration.default()
) {
    private val suggestionService = ErrorRecoverySuggestionService(configuration)

    /**
     * Categorizes an error by its recoverability level.
     * All errors now require user consent - no automatic fixes without permission.
     */
    fun categorizeError(error: DomainError): ErrorRecoveryCategory {
        return when (error) {
            is DomainError.ValidationError -> categorizeValidationError(error)
            is DomainError.BusinessRuleViolation -> categorizeBusinessRuleViolation(error)
            is DomainError.ScopeError -> ErrorRecoveryCategory.NON_RECOVERABLE
        }
    }

    /**
     * Categorizes validation errors.
     */
    private fun categorizeValidationError(error: DomainError.ValidationError): ErrorRecoveryCategory {
        return when (error) {
            // Validation errors are partially recoverable - provide suggestions that require user approval
            is DomainError.ValidationError.EmptyTitle,
            is DomainError.ValidationError.TitleTooShort,
            is DomainError.ValidationError.TitleTooLong,
            is DomainError.ValidationError.TitleContainsNewline,
            is DomainError.ValidationError.DescriptionTooLong -> ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
            
            // Unknown validation errors default to non-recoverable for safety
            is DomainError.ValidationError.InvalidFormat -> ErrorRecoveryCategory.NON_RECOVERABLE
        }
    }

    /**
     * Categorizes business rule violations.
     */
    private fun categorizeBusinessRuleViolation(error: DomainError.BusinessRuleViolation): ErrorRecoveryCategory {
        return when (error) {
            // Business rule violations are partially recoverable - can suggest fixes but require user input
            is DomainError.BusinessRuleViolation.DuplicateTitle,
            is DomainError.BusinessRuleViolation.MaxDepthExceeded,
            is DomainError.BusinessRuleViolation.MaxChildrenExceeded -> ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
        }
    }

    /**
     * Attempts to provide suggestions for error recovery using appropriate strategy.
     * All recovery now requires explicit user consent - no automatic fixes.
     */
    fun recoverFromError(
        error: DomainError,
        context: Map<String, Any> = emptyMap()
    ): RecoveryResult? {
        val category = categorizeError(error)

        return when (category) {
            ErrorRecoveryCategory.PARTIALLY_RECOVERABLE -> suggestRecovery(error, context)
            ErrorRecoveryCategory.NON_RECOVERABLE -> suggestionService.suggestRecovery(error, context)
        }
    }

    /**
     * Attempts to recover from multiple errors.
     */
    fun recoverFromErrors(
        errors: List<DomainError>,
        context: Map<String, Any> = emptyMap()
    ): List<RecoveryResult> {
        return errors.mapNotNull { error ->
            recoverFromError(error, context)
        }
    }

    /**
     * Integrates recovery with ValidationResult system.
     */
    fun recoverFromValidationResult(
        result: ValidationResult<*>,
        context: Map<String, Any> = emptyMap()
    ): RecoveredValidationResult<*> {
        return when (result) {
            is ValidationResult.Success -> RecoveredValidationResult(result, emptyList())
            is ValidationResult.Failure -> {
                val recoveryResults = recoverFromErrors(result.errors.toList(), context)
                RecoveredValidationResult(result, recoveryResults)
            }
        }
    }

    // ===== PRIVATE RECOVERY STRATEGIES =====

    /**
     * Handles partially recoverable errors by suggesting fixes.
     * Now includes all previously "automatic" recoveries as suggestions.
     */
    private fun suggestRecovery(
        error: DomainError,
        context: Map<String, Any>
    ): RecoveryResult {
        return suggestionService.suggestRecovery(error, context)
    }

}
