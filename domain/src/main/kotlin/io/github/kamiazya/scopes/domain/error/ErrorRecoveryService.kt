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
    private val configuration: RecoveryConfiguration = RecoveryConfiguration()
) {

    companion object {
        private const val WORD_REDUCTION_FACTOR = 10
        private const val MAX_UNIQUE_VARIANTS = 3
    }

    /**
     * Categorizes an error by its recoverability level.
     * All errors now require user consent - no automatic fixes without permission.
     */
    fun categorizeError(error: DomainError): ErrorRecoveryCategory {
        return when (error) {
            // Previously "recoverable" errors - now only provide suggestions that require user approval
            is DomainError.ValidationError.EmptyTitle -> ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
            is DomainError.ValidationError.TitleTooShort -> ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
            is DomainError.ValidationError.TitleTooLong -> ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
            is DomainError.ValidationError.TitleContainsNewline -> ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
            is DomainError.ValidationError.DescriptionTooLong -> ErrorRecoveryCategory.PARTIALLY_RECOVERABLE

            // Partially recoverable errors - can suggest fixes but require user input
            is DomainError.BusinessRuleViolation.DuplicateTitle -> ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
            is DomainError.BusinessRuleViolation.MaxDepthExceeded -> ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
            is DomainError.BusinessRuleViolation.MaxChildrenExceeded -> ErrorRecoveryCategory.PARTIALLY_RECOVERABLE

            // Non-recoverable errors - require manual resolution
            is DomainError.ScopeError.CircularReference -> ErrorRecoveryCategory.NON_RECOVERABLE
            is DomainError.ScopeError.SelfParenting -> ErrorRecoveryCategory.NON_RECOVERABLE
            is DomainError.ScopeError.ScopeNotFound -> ErrorRecoveryCategory.NON_RECOVERABLE
            is DomainError.ScopeError.InvalidTitle -> ErrorRecoveryCategory.NON_RECOVERABLE
            is DomainError.ScopeError.InvalidDescription -> ErrorRecoveryCategory.NON_RECOVERABLE
            is DomainError.ScopeError.InvalidParent -> ErrorRecoveryCategory.NON_RECOVERABLE

            // Unknown errors default to non-recoverable for safety
            is DomainError.ValidationError.InvalidFormat -> ErrorRecoveryCategory.NON_RECOVERABLE
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
            ErrorRecoveryCategory.NON_RECOVERABLE -> handleNonRecoverable(error)
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
        return when (error) {
            is DomainError.ValidationError.EmptyTitle -> suggestEmptyTitleRecovery(error)
            is DomainError.ValidationError.TitleTooShort -> suggestTitleTooShortRecovery(error, context)
            is DomainError.ValidationError.TitleTooLong -> suggestTitleTooLongRecovery(error, context)
            is DomainError.ValidationError.TitleContainsNewline -> suggestTitleContainsNewlineRecovery(error, context)
            is DomainError.ValidationError.DescriptionTooLong -> suggestDescriptionTooLongRecovery(error, context)
            is DomainError.BusinessRuleViolation.DuplicateTitle -> suggestDuplicateTitleRecovery(error, context)
            is DomainError.BusinessRuleViolation.MaxDepthExceeded -> suggestMaxDepthExceededRecovery(error)
            is DomainError.BusinessRuleViolation.MaxChildrenExceeded -> suggestMaxChildrenExceededRecovery(error)
            else -> handleNonRecoverable(error)
        }
    }

    /**
     * Suggests recovery for empty title validation errors.
     */
    private fun suggestEmptyTitleRecovery(error: DomainError.ValidationError.EmptyTitle): RecoveryResult {
        return RecoveryResult.Suggestion(
            originalError = error,
            suggestedValues = listOf(configuration.defaultTitleTemplate),
            strategy = "DefaultTitle",
            description = "Title cannot be empty. Consider using a default title to get started quickly."
        )
    }

    /**
     * Suggests recovery for title too short validation errors.
     */
    private fun suggestTitleTooShortRecovery(
        error: DomainError.ValidationError.TitleTooShort,
        context: Map<String, Any>
    ): RecoveryResult {
        val originalTitle = context["originalTitle"] as? String ?: ""
        val suggestions = if (originalTitle.isNotBlank()) {
            listOf(
                "$originalTitle - Task",
                "$originalTitle - Item",
                "TODO: $originalTitle"
            )
        } else {
            listOf("New Task", "New Item", "TODO Item")
        }

        return RecoveryResult.Suggestion(
            originalError = error,
            suggestedValues = suggestions,
            strategy = "PadTitle",
            description = "Title is too short. Here are some ways to make it more descriptive."
        )
    }

    /**
     * Suggests recovery for title too long validation errors.
     */
    private fun suggestTitleTooLongRecovery(
        error: DomainError.ValidationError.TitleTooLong,
        context: Map<String, Any>
    ): RecoveryResult {
        val originalTitle = context["originalTitle"] as? String ?: ""
        val maxLength = configuration.maxTitleLength
        val suffixLength = configuration.truncationSuffix.length

        val suggestions = if (originalTitle.isNotBlank()) {
            listOf(
                originalTitle.take(maxLength - suffixLength) + configuration.truncationSuffix,
                originalTitle.take(maxLength),
                originalTitle.split(" ")
                    .take(maxLength / WORD_REDUCTION_FACTOR)
                    .joinToString(" ") // Take first few words
            )
        } else {
            listOf(configuration.defaultTitleTemplate)
        }

        return RecoveryResult.Suggestion(
            originalError = error,
            suggestedValues = suggestions,
            strategy = "TruncateTitle",
            description = "Title exceeds maximum length (${maxLength} characters). " +
                "Here are shortened versions that preserve meaning."
        )
    }

    /**
     * Suggests recovery for title contains newline validation errors.
     */
    private fun suggestTitleContainsNewlineRecovery(
        error: DomainError.ValidationError.TitleContainsNewline,
        context: Map<String, Any>
    ): RecoveryResult {
        val originalTitle = context["originalTitle"] as? String ?: ""
        val suggestions = if (originalTitle.isNotBlank()) {
            listOf(
                originalTitle.replace("\n", " ").replace("\r", " ").replace(Regex("\\s+"), " ").trim(),
                originalTitle.replace("\n", " - ")
                    .replace("\r", " - ")
                    .replace(Regex("\\s+-\\s+"), " - ")
                    .trim(),
                originalTitle.split(Regex("[\\n\\r]+")).first().trim() // Just take first line
            ).filter { it.isNotBlank() }
        } else {
            listOf(configuration.defaultTitleTemplate)
        }

        return RecoveryResult.Suggestion(
            originalError = error,
            suggestedValues = suggestions,
            strategy = "CleanTitle",
            description = "Titles cannot contain line breaks. " +
                "Here are cleaned versions that preserve your content."
        )
    }

    /**
     * Suggests recovery for description too long validation errors.
     */
    private fun suggestDescriptionTooLongRecovery(
        error: DomainError.ValidationError.DescriptionTooLong,
        context: Map<String, Any>
    ): RecoveryResult {
        val originalDescription = context["originalDescription"] as? String ?: ""
        val maxLength = configuration.maxDescriptionLength
        val suffixLength = configuration.truncationSuffix.length

        val suggestions = if (originalDescription.isNotBlank()) {
            listOf(
                originalDescription.take(maxLength - suffixLength) + configuration.truncationSuffix,
                originalDescription.split(".").first().trim(), // Take first sentence
                originalDescription.split("\n").first().trim()  // Take first paragraph
            ).filter { it.isNotBlank() }.distinct()
        } else {
            listOf("") // Empty description is valid
        }

        return RecoveryResult.Suggestion(
            originalError = error,
            suggestedValues = suggestions,
            strategy = "TruncateDescription",
            description = "Description exceeds maximum length (${maxLength} characters). " +
                "Here are shortened versions that preserve key information."
        )
    }

    /**
     * Suggests recovery for duplicate title business rule violations.
     */
    private fun suggestDuplicateTitleRecovery(
        error: DomainError.BusinessRuleViolation.DuplicateTitle,
        context: Map<String, Any>
    ): RecoveryResult {
        val originalTitle = context["originalTitle"] as? String ?: error.title
        val parentId = context["parentId"] as? ScopeId
        val allScopes = context["allScopes"] as? List<Scope> ?: emptyList()

        val suggestions = generateUniqueVariants(originalTitle, parentId, allScopes)

        return RecoveryResult.Suggestion(
            originalError = error,
            suggestedValues = suggestions,
            strategy = "AppendNumber",
            description = "A scope with this title already exists. " +
                "Here are unique variations you can use instead."
        )
    }

    /**
     * Suggests recovery for max depth exceeded business rule violations.
     */
    private fun suggestMaxDepthExceededRecovery(
        error: DomainError.BusinessRuleViolation.MaxDepthExceeded
    ): RecoveryResult {
        return RecoveryResult.Suggestion(
            originalError = error,
            suggestedValues = listOf(
                "Move this scope to a higher level in the hierarchy",
                "Consider restructuring parent scopes to reduce nesting",
                "Create a separate top-level scope for this content"
            ),
            strategy = "ReorganizeHierarchy",
            description = "The hierarchy is too deep (maximum ${error.maxDepth} levels). " +
                "Consider restructuring to reduce nesting."
        )
    }

    /**
     * Suggests recovery for max children exceeded business rule violations.
     */
    private fun suggestMaxChildrenExceededRecovery(
        error: DomainError.BusinessRuleViolation.MaxChildrenExceeded
    ): RecoveryResult {
        return RecoveryResult.Suggestion(
            originalError = error,
            suggestedValues = listOf(
                "Create intermediate grouping scopes to organize children",
                "Move some children to different parent scopes",
                "Combine related children into sub-categories"
            ),
            strategy = "CreateGrouping",
            description = "Too many child scopes (maximum ${error.maxChildren} allowed). " +
                "Consider organizing them into logical groups."
        )
    }

    /**
     * Handles non-recoverable errors.
     */
    private fun handleNonRecoverable(error: DomainError): RecoveryResult {
        val reason = when (error) {
            is DomainError.ScopeError.CircularReference ->
                "Circular references require manual resolution to prevent infinite loops"
            is DomainError.ScopeError.SelfParenting ->
                "Self-parenting violates fundamental hierarchy rules and cannot be automatically fixed"
            is DomainError.ScopeError.ScopeNotFound ->
                "Missing scope must be created or reference must be updated manually"
            else ->
                "This error type requires manual intervention and cannot be automatically recovered"
        }

        return RecoveryResult.NonRecoverable(
            originalError = error,
            reason = reason
        )
    }

    /**
     * Generates unique title variants for duplicate title resolution.
     */
    private fun generateUniqueVariants(
        originalTitle: String,
        parentId: ScopeId?,
        allScopes: List<Scope>,
        maxVariants: Int = 10
    ): List<String> {
        val existingTitles = allScopes
            .filter { it.parentId == parentId }
            .map { it.title.value.lowercase() }
            .toSet()

        val variants = mutableListOf<String>()

        for (i in 1..maxVariants) {
            val variant = when {
                configuration.duplicateTitleSuffix.contains("{number}") ->
                    originalTitle + configuration.duplicateTitleSuffix.replace("{number}", i.toString())
                else ->
                    originalTitle + configuration.duplicateTitleSuffix + i
            }

            if (variant.lowercase() !in existingTitles) {
                variants.add(variant)
                if (variants.size >= MAX_UNIQUE_VARIANTS) {
                    break // Provide up to 3 suggestions
                }
            }
        }

        return variants.ifEmpty { listOf("$originalTitle (Copy)") }
    }
}
