package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.RecoveryStrategy
import io.github.kamiazya.scopes.domain.error.RecoveryApproach

/**
 * Pure domain service responsible for determining recovery strategies and approaches.
 *
 * This service contains only pure domain logic for mapping error types to recovery strategies
 * and approaches. It has no external dependencies, no configuration, and no side effects.
 * All methods are pure functions that express domain concepts a domain expert can understand.
 *
 * Key Principles:
 * - Stateless and pure functions only
 * - No configuration dependencies
 * - No application or infrastructure concerns
 * - Expresses business concepts and domain expertise
 * - Zero external dependencies beyond domain types
 */
class RecoveryStrategyDomainService {

    /**
     * Determines the appropriate recovery strategy for a given domain error.
     *
     * This pure function maps error types to recovery strategies based on domain logic.
     * Each strategy represents a domain concept for how the error should be approached.
     *
     * @param error The domain error to determine strategy for
     * @return The recovery strategy that best fits this error type from domain perspective
     */
    fun determineRecoveryStrategy(error: DomainError): RecoveryStrategy {
        return when (error) {
            // Validation errors that need default values
            is DomainError.ScopeValidationError.EmptyScopeTitle,
            is DomainError.ScopeValidationError.ScopeTitleTooShort ->
                RecoveryStrategy.DEFAULT_VALUE

            // Validation errors that need truncation
            is DomainError.ScopeValidationError.ScopeTitleTooLong,
            is DomainError.ScopeValidationError.ScopeDescriptionTooLong ->
                RecoveryStrategy.TRUNCATE

            // Validation errors that need format cleaning
            is DomainError.ScopeValidationError.ScopeTitleContainsNewline,
            is DomainError.ScopeValidationError.ScopeInvalidFormat ->
                RecoveryStrategy.CLEAN_FORMAT

            // Business rule violations that need variant generation
            is DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle ->
                RecoveryStrategy.GENERATE_VARIANTS

            // Business rule violations that need hierarchy restructuring
            // ScopeMaxDepthExceeded consolidated into BusinessRuleServiceError.ScopeBusinessRuleError.MaxDepthExceeded
            // is DomainError.ScopeBusinessRuleViolation.ScopeMaxDepthExceeded,
            is DomainError.ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded ->
                RecoveryStrategy.RESTRUCTURE_HIERARCHY

            // Format errors, scope errors, and infrastructure errors are not handled by this service
            // as they typically require manual intervention beyond strategy determination
            else -> throw IllegalArgumentException(
                "No recovery strategy defined for error type: ${error::class.simpleName}"
            )
        }
    }

    /**
     * Determines the approach needed to implement a recovery strategy for a given error.
     *
     * This pure function maps error types to recovery approaches based on domain complexity
     * and the level of user involvement required from a domain perspective.
     *
     * @param error The domain error to determine approach for
     * @return The recovery approach that best fits this error type from domain perspective
     */
    fun getStrategyApproach(error: DomainError): RecoveryApproach {
        return when (error) {
            // Simple errors where system can confidently suggest exact values
            is DomainError.ScopeValidationError.EmptyScopeTitle,
            is DomainError.ScopeValidationError.ScopeTitleTooShort ->
                RecoveryApproach.AUTOMATIC_SUGGESTION

            // Moderate errors where system can suggest options but user choice is needed
            is DomainError.ScopeValidationError.ScopeTitleTooLong,
            is DomainError.ScopeValidationError.ScopeTitleContainsNewline,
            is DomainError.ScopeValidationError.ScopeInvalidFormat,
            is DomainError.ScopeValidationError.ScopeDescriptionTooLong,
            is DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle ->
                RecoveryApproach.USER_INPUT_REQUIRED

            // Complex errors where significant manual intervention is needed
            // ScopeMaxDepthExceeded consolidated into BusinessRuleServiceError.ScopeBusinessRuleError.MaxDepthExceeded
            // is DomainError.ScopeBusinessRuleViolation.ScopeMaxDepthExceeded,
            is DomainError.ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded ->
                RecoveryApproach.MANUAL_INTERVENTION

            // Scope errors require manual intervention
            is DomainError.ScopeError -> RecoveryApproach.MANUAL_INTERVENTION

            // Infrastructure errors require manual intervention
            is DomainError.InfrastructureError -> RecoveryApproach.MANUAL_INTERVENTION

            // Format errors and other unhandled cases
            else -> throw IllegalArgumentException(
                "No recovery approach defined for error type: ${error::class.simpleName}"
            )
        }
    }

    /**
     * Determines if a recovery strategy is complex from a domain perspective.
     *
     * This pure function categorizes strategies based on the inherent complexity
     * of implementing them. Complex strategies typically require more user involvement,
     * external data, or sophisticated logic.
     *
     * @param strategy The recovery strategy to assess
     * @return true if the strategy is complex, false if it's simple
     */
    fun isStrategyComplex(strategy: RecoveryStrategy): Boolean {
        return when (strategy) {
            // Simple strategies - direct, deterministic, minimal user input
            RecoveryStrategy.DEFAULT_VALUE,
            RecoveryStrategy.TRUNCATE,
            RecoveryStrategy.CLEAN_FORMAT -> false

            // Complex strategies - require external data, user choices, or restructuring
            RecoveryStrategy.GENERATE_VARIANTS,
            RecoveryStrategy.RESTRUCTURE_HIERARCHY -> true
        }
    }
}
