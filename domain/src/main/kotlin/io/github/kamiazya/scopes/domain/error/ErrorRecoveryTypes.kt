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
 * Configuration for error recovery suggestions.
 * Note: All recovery is now suggestion-based - no automatic fixes without user consent.
 */
data class RecoveryConfiguration(
    val defaultTitleTemplate: String = "Untitled Scope",
    val maxTitleLength: Int = 200,
    val maxDescriptionLength: Int = 1000,
    val duplicateTitleSuffix: String = " ({number})",
    val truncationSuffix: String = "..."
)

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
