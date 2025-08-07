package io.github.kamiazya.scopes.domain.error

/**
 * Error Recovery Extensions
 *
 * Extension functions that provide convenient integration points
 * for the error recovery system with existing validation workflows.
 */

/**
 * Extension function for ValidationResult to easily apply error recovery.
 */
fun <T> ValidationResult<T>.withRecovery(
    recoveryService: ErrorRecoveryService = ErrorRecoveryService(),
    context: Map<String, Any> = emptyMap()
): RecoveredValidationResult<*> {
    return recoveryService.recoverFromValidationResult(this, context)
}

/**
 * Extension function to get a summary of recovery results.
 */
fun RecoveredValidationResult<*>.getRecoverySummary(): RecoverySummary {
    val suggestions = recoveryResults.filterIsInstance<RecoveryResult.Suggestion>()
    val nonRecoverable = recoveryResults.filterIsInstance<RecoveryResult.NonRecoverable>()

    return RecoverySummary(
        totalErrors = recoveryResults.size,
        automaticallyRecovered = 0, // No automatic recovery anymore
        requiresUserInput = suggestions.size,
        cannotRecover = nonRecoverable.size,
        recoveryStrategies = recoveryResults.map { it.getStrategyName() }.toSet(),
        hasCompleteRecovery = false // Never true - no automatic fixes
    )
}

/**
 * Extension function to get strategy name from any RecoveryResult.
 */
private fun RecoveryResult.getStrategyName(): String = when (this) {
    is RecoveryResult.Success -> strategy
    is RecoveryResult.Suggestion -> strategy
    is RecoveryResult.NonRecoverable -> "NonRecoverable"
}

/**
 * Extension function for DomainError to quickly check recoverability.
 */
fun DomainError.getRecoverabilityLevel(
    recoveryService: ErrorRecoveryService = ErrorRecoveryService()
): ErrorRecoveryCategory {
    return recoveryService.categorizeError(this)
}

/**
 * Extension function for List<DomainError> to get recovery statistics.
 */
fun List<DomainError>.getRecoveryStatistics(
    recoveryService: ErrorRecoveryService = ErrorRecoveryService()
): RecoveryStatistics {
    val categories = this.map { recoveryService.categorizeError(it) }

    return RecoveryStatistics(
        totalErrors = this.size,
        recoverableCount = 0, // No automatic recovery anymore
        partiallyRecoverableCount = categories.count { it == ErrorRecoveryCategory.PARTIALLY_RECOVERABLE },
        nonRecoverableCount = categories.count { it == ErrorRecoveryCategory.NON_RECOVERABLE },
        recoverabilityRate = if (this.isEmpty()) 0.0 else {
            categories.count { it != ErrorRecoveryCategory.NON_RECOVERABLE }.toDouble() / this.size
        }
    )
}

/**
 * Summary of recovery results for reporting and logging.
 * Note: No automatic recovery anymore - all fixes require user consent.
 */
data class RecoverySummary(
    val totalErrors: Int,
    val automaticallyRecovered: Int = 0, // Always 0 - no automatic recovery
    val requiresUserInput: Int,
    val cannotRecover: Int,
    val recoveryStrategies: Set<String>,
    val hasCompleteRecovery: Boolean = false // Never true - no automatic fixes
) {
    /**
     * Human-readable summary for logging or user display.
     */
    fun toReadableString(): String = buildString {
        append("Recovery Summary: ")
        append("$requiresUserInput/$totalErrors suggestions available")
        if (cannotRecover > 0) append(", $cannotRecover cannot recover")
        append(" (strategies: ${recoveryStrategies.joinToString()})")
    }
}

/**
 * Statistics about error recoverability for analysis.
 */
data class RecoveryStatistics(
    val totalErrors: Int,
    val recoverableCount: Int,
    val partiallyRecoverableCount: Int,
    val nonRecoverableCount: Int,
    val recoverabilityRate: Double
) {
    /**
     * Percentage of errors that can be at least partially recovered.
     */
    val recoverabilityPercentage: Double = recoverabilityRate * 100.0

    /**
     * Human-readable statistics for reporting.
     */
    fun toReadableString(): String = buildString {
        append("Recovery Statistics: ")
        append("${recoverabilityPercentage.format(1)}% have suggestions ")
        append("($partiallyRecoverableCount suggestions, $nonRecoverableCount manual)")
    }
}

/**
 * Extension function to format double with specified decimal places.
 */
private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)
