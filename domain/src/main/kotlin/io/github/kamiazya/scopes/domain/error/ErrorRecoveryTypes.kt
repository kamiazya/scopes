package io.github.kamiazya.scopes.domain.error

/**
 * Error Recovery Types
 *
 * This file contains the core types for the error recovery system,
 * which provides helpful suggestions for common validation failures
 * while requiring explicit user consent for all modifications.
 */

/**
 * Categorizes errors by their recoverability level.
 * Note: All errors now require user consent - no automatic fixes without permission.
 */
enum class ErrorRecoveryCategory {
    /**
     * Errors that can be corrected with suggestions that require user approval or input.
     * Examples: Empty title -> suggest "Untitled Scope", Title too long -> suggest truncation,
     *           Duplicate title -> suggest variants, Max depth exceeded -> suggest moving
     */
    PARTIALLY_RECOVERABLE,

    /**
     * Errors that require manual resolution and cannot be automatically fixed.
     * Examples: Circular references, Self-parenting
     */
    NON_RECOVERABLE
}

/**
 * Domain concept representing recovery complexity levels.
 * 
 * This enum categorizes the complexity involved in recovering from errors
 * from a domain perspective, helping determine the appropriate recovery strategy.
 */
enum class RecoveryComplexity {
    /**
     * Simple recovery where direct suggestions are possible.
     * Examples: Empty title -> suggest default, Title too short -> suggest expansion
     */
    SIMPLE,

    /**
     * Moderate recovery that requires user input or choices.
     * Examples: Duplicate title -> offer multiple variants, Description too long -> offer truncation options
     */
    MODERATE,

    /**
     * Complex recovery requiring significant user intervention.
     * Examples: Max depth exceeded -> restructure hierarchy, Circular references -> manual resolution
     */
    COMPLEX
}

/**
 * Represents the result of an error recovery attempt in the suggestion-only system.
 * All recovery now requires user consent - no automatic fixes without permission.
 */
sealed class RecoveryResult {
    abstract val originalError: DomainError

    /**
     * Partial recovery with suggested corrections that require user approval.
     */
    data class Suggestion(
        override val originalError: DomainError,
        val suggestedValues: List<Any>,
        val strategy: String,
        val description: String = ""
    ) : RecoveryResult()

    /**
     * Error cannot be automatically recovered and requires manual resolution.
     */
    data class NonRecoverable(
        override val originalError: DomainError,
        val reason: String
    ) : RecoveryResult()
}


/**
 * Domain concept representing specific recovery strategies for different error types.
 * 
 * Each strategy represents a pure domain approach to recovering from a specific 
 * category of error. These strategies are determined by domain logic and express
 * business concepts that domain experts can understand and validate.
 */
enum class RecoveryStrategy {
    /**
     * Provide a default value when none exists.
     * Used for: Empty titles, missing required fields
     */
    DEFAULT_VALUE,

    /**
     * Truncate content to fit within constraints while preserving meaning.
     * Used for: Content too long (titles, descriptions)
     */
    TRUNCATE,

    /**
     * Clean and format content to meet validation requirements.
     * Used for: Invalid formatting (newlines in titles, whitespace issues)
     */
    CLEAN_FORMAT,

    /**
     * Generate multiple alternative variants for user selection.
     * Used for: Duplicate titles, conflicting names
     */
    GENERATE_VARIANTS,

    /**
     * Restructure hierarchy to resolve constraint violations.
     * Used for: Max depth exceeded, max children exceeded
     */
    RESTRUCTURE_HIERARCHY
}

/**
 * Domain concept representing the approach needed for recovery implementation.
 * 
 * This enum categorizes how a recovery strategy should be applied from
 * a domain perspective, determining the level of user involvement required.
 */
enum class RecoveryApproach {
    /**
     * System can automatically suggest specific values with high confidence.
     * Domain logic can determine exact suggestions without additional input.
     */
    AUTOMATIC_SUGGESTION,

    /**
     * User input or choice is required to complete the recovery.
     * Domain logic can provide options but needs user decision.
     */
    USER_INPUT_REQUIRED,

    /**
     * Complex manual intervention needed beyond simple suggestions.
     * Domain logic identifies the issue but resolution requires significant user work.
     */
    MANUAL_INTERVENTION
}

/**
 * Enhanced ValidationResult that includes recovery information.
 * Integrates the recovery system with the existing validation framework.
 */
data class RecoveredValidationResult<T>(
    val originalResult: ValidationResult<T>,
    val recoveryResults: List<RecoveryResult>
) {
    /**
     * Checks if any errors have suggested recoveries.
     */
    fun hasAnySuggestions(): Boolean =
        recoveryResults.any { it is RecoveryResult.Suggestion }

    /**
     * Gets all suggested values that require user approval.
     */
    fun getAllSuggestedValues(): List<Any> =
        recoveryResults.filterIsInstance<RecoveryResult.Suggestion>()
            .flatMap { it.suggestedValues }

    /**
     * Gets all non-recoverable errors that require manual resolution.
     */
    fun getNonRecoverableErrors(): List<DomainError> =
        recoveryResults.filterIsInstance<RecoveryResult.NonRecoverable>()
            .map { it.originalError }

}
