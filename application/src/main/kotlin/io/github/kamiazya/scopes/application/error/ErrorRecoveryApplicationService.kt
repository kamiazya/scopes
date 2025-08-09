package io.github.kamiazya.scopes.application.error

import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.ErrorFormatter
import io.github.kamiazya.scopes.domain.service.ErrorRecoveryDomainService
import io.github.kamiazya.scopes.domain.service.ErrorRecoverySuggestionService
import io.github.kamiazya.scopes.domain.service.SuggestionContext
import io.github.kamiazya.scopes.domain.service.RecoveryStrategyDomainService
import io.github.kamiazya.scopes.domain.error.ErrorRecoveryCategory
import io.github.kamiazya.scopes.domain.error.RecoveryResult
import io.github.kamiazya.scopes.domain.error.RecoveredValidationResult
import io.github.kamiazya.scopes.domain.error.ValidationResult
import io.github.kamiazya.scopes.domain.error.ScopeRecoveryConfiguration
import io.github.kamiazya.scopes.domain.error.RecoveryStrategy
import io.github.kamiazya.scopes.domain.error.RecoveryApproach

/**
 * Application service that orchestrates error recovery workflows.
 *
 * This service coordinates between multiple domain services to provide complete error recovery
 * functionality. It contains no domain logic itself but orchestrates the flow between pure
 * domain services and handles application-level concerns like configuration management.
 *
 * Responsibilities:
 * - Orchestrate error recovery workflows using domain services
 * - Coordinate between error categorization, strategy determination, and suggestion generation
 * - Handle ValidationResult integration workflows
 * - Manage context passing and configuration between services
 * - Ensure proper separation of concerns between domain and application layers
 *
 * Architecture:
 * - Uses ErrorRecoveryDomainService for error categorization (pure domain logic)
 * - Uses RecoveryStrategyDomainService for strategy determination (pure domain logic)
 * - Uses ErrorRecoverySuggestionService for concrete suggestion generation (domain + config)
 * - Handles infrastructure concerns like error formatting
 */
class ErrorRecoveryApplicationService(
    private val errorCategorizationService: ErrorRecoveryDomainService,
    private val recoveryStrategyService: RecoveryStrategyDomainService,
    private val errorFormatter: ErrorFormatter,
    private val configuration: ScopeRecoveryConfiguration.Complete = ScopeRecoveryConfiguration.default()
) {
    // Domain service for suggestion generation - created with configuration
    private val suggestionService = ErrorRecoverySuggestionService(configuration)

    /**
     * Orchestrates complete error recovery process.
     *
     * This method coordinates between multiple domain services:
     * 1. Uses ErrorRecoveryDomainService to categorize the error
     * 2. Uses ErrorRecoverySuggestionService to generate concrete suggestions
     * 3. Handles non-recoverable cases with proper infrastructure formatting
     */
    fun recoverFromError(
        error: DomainError,
        context: SuggestionContext = SuggestionContext.NoContext
    ): RecoveryResult {
        // Step 1: Use pure domain service for error categorization
        val category = errorCategorizationService.categorizeError(error)

        return when (category) {
            ErrorRecoveryCategory.PARTIALLY_RECOVERABLE -> {
                // Generate concrete suggestions using domain service with configuration
                suggestionService.suggestRecovery(error, context)
            }
            ErrorRecoveryCategory.NON_RECOVERABLE -> {
                // Handle non-recoverable errors with infrastructure formatting
                val message = errorFormatter.getErrorMessage(error)
                RecoveryResult.NonRecoverable(
                    originalError = error,
                    reason = "Error cannot be automatically recovered: $message"
                )
            }
        }
    }

    /**
     * Handles ValidationResult recovery workflow using proper DDD orchestration.
     *
     * Coordinates recovery for each error in a ValidationResult by delegating
     * to the main recovery orchestration method.
     */
    fun recoverFromValidationResult(
        result: ValidationResult<*>,
        context: SuggestionContext = SuggestionContext.NoContext
    ): RecoveredValidationResult<*> {
        return when (result) {
            is ValidationResult.Success -> {
                // Success cases don't need recovery
                RecoveredValidationResult(result, emptyList())
            }
            is ValidationResult.Failure -> {
                // For failures, orchestrate recovery for each error using domain services
                val recoveryResults = result.errors.toList().map { error ->
                    recoverFromError(error, context)
                }
                RecoveredValidationResult(result, recoveryResults)
            }
        }
    }

    // ===== ADDITIONAL ORCHESTRATION METHODS =====

    /**
     * Provides strategy information for an error without generating concrete suggestions.
     * Useful for UI/UX layer to understand recovery complexity before proceeding.
     */
    fun getRecoveryStrategyInfo(error: DomainError): Triple<RecoveryStrategy, RecoveryApproach, Boolean> {
        val strategy = recoveryStrategyService.determineRecoveryStrategy(error)
        val approach = recoveryStrategyService.getStrategyApproach(error)
        val isComplex = recoveryStrategyService.isStrategyComplex(strategy)

        return Triple(strategy, approach, isComplex)
    }

    /**
     * Checks if an error is recoverable without generating full recovery suggestions.
     * Efficient method for quick recoverability assessment.
     */
    fun isErrorRecoverable(error: DomainError): Boolean {
        return errorCategorizationService.isRecoverable(error)
    }
}
