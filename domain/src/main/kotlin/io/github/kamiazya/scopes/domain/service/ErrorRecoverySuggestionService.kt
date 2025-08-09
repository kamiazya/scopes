package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.RecoveryResult
import io.github.kamiazya.scopes.domain.error.RecoveryStrategy
import io.github.kamiazya.scopes.domain.error.ScopeRecoveryConfiguration
import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Domain service that generates recovery suggestions for errors.
 */
@Suppress("TooManyFunctions") // This class is designed to contain many suggestion functions
class ErrorRecoverySuggestionService(
    private val configuration: ScopeRecoveryConfiguration.Complete
) {

    companion object {
        private const val WORD_REDUCTION_FACTOR = 10
        private const val MAX_UNIQUE_VARIANTS = 3
    }

    /**
     * Suggests recovery options for domain errors.
     */
    @Suppress("CyclomaticComplexMethod") // Exhaustive when requires all cases to be handled
    fun suggestRecovery(
        error: DomainError,
        context: Map<String, Any>
    ): RecoveryResult {
        return when (error) {
            is DomainError.ScopeValidationError.EmptyScopeTitle -> suggestEmptyTitleRecovery(error)
            is DomainError.ScopeValidationError.ScopeTitleTooShort -> suggestTitleTooShortRecovery(error, context)
            is DomainError.ScopeValidationError.ScopeTitleTooLong -> suggestTitleTooLongRecovery(error, context)
            is DomainError.ScopeValidationError.ScopeTitleContainsNewline ->
                suggestTitleContainsNewlineRecovery(error, context)
            is DomainError.ScopeValidationError.ScopeDescriptionTooLong ->
                suggestDescriptionTooLongRecovery(error, context)
            is DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle ->
                suggestDuplicateTitleRecovery(error, context)
            is DomainError.ScopeBusinessRuleViolation.ScopeMaxDepthExceeded -> suggestMaxDepthExceededRecovery(error)
            is DomainError.ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded ->
                suggestMaxChildrenExceededRecovery(error)
            is DomainError.ScopeValidationError.ScopeInvalidFormat -> handleNonRecoverable(error)
            is DomainError.ScopeError.CircularReference -> handleNonRecoverable(error)
            is DomainError.ScopeError.SelfParenting -> handleNonRecoverable(error)
            is DomainError.ScopeError.ScopeNotFound -> handleNonRecoverable(error)
            is DomainError.ScopeError.InvalidTitle -> handleNonRecoverable(error)
            is DomainError.ScopeError.InvalidDescription -> handleNonRecoverable(error)
            is DomainError.ScopeError.InvalidParent -> handleNonRecoverable(error)
            is DomainError.InfrastructureError -> handleNonRecoverable(error)
        }
    }

    /**
     * Suggests recovery for empty title validation errors.
     */
    private fun suggestEmptyTitleRecovery(error: DomainError.ScopeValidationError.EmptyScopeTitle): RecoveryResult {
        return RecoveryResult.Suggestion(
            originalError = error,
            suggestedValues = listOf(configuration.titleConfig().generateDefaultTitle()),
            strategy = RecoveryStrategy.DEFAULT_VALUE,
            description = "Title cannot be empty. Consider using a default title to get started quickly."
        )
    }

    /**
     * Suggests recovery for title too short validation errors.
     */
    private fun suggestTitleTooShortRecovery(
        error: DomainError.ScopeValidationError.ScopeTitleTooShort,
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
            strategy = RecoveryStrategy.DEFAULT_VALUE,
            description = "Title is too short. Here are some ways to make it more descriptive."
        )
    }

    /**
     * Suggests recovery for title too long validation errors.
     */
    private fun suggestTitleTooLongRecovery(
        error: DomainError.ScopeValidationError.ScopeTitleTooLong,
        context: Map<String, Any>
    ): RecoveryResult {
        val originalTitle = context["originalTitle"] as? String ?: ""
        val titleConfig = configuration.titleConfig()
        val maxLength = titleConfig.maxLength

        val suggestions = if (originalTitle.isNotBlank()) {
            val candidateSuggestions = listOf(
                titleConfig.truncateTitle(originalTitle),
                originalTitle.take(maxLength),
                originalTitle.split(" ")
                    .take(maxOf(1, maxLength / WORD_REDUCTION_FACTOR))
                    .joinToString(" ") // Take at least first word
            )

            // Filter out blank suggestions and ensure at least one valid suggestion
            val validSuggestions = candidateSuggestions.filter { it.isNotBlank() }
            if (validSuggestions.isEmpty()) {
                listOf(titleConfig.generateDefaultTitle())
            } else {
                validSuggestions
            }
        } else {
            listOf(titleConfig.generateDefaultTitle())
        }

        return RecoveryResult.Suggestion(
            originalError = error,
            suggestedValues = suggestions,
            strategy = RecoveryStrategy.TRUNCATE,
            description = "Title exceeds maximum length (${maxLength} characters). " +
                "Here are shortened versions that preserve meaning."
        )
    }

    /**
     * Suggests recovery for title contains newline validation errors.
     */
    private fun suggestTitleContainsNewlineRecovery(
        error: DomainError.ScopeValidationError.ScopeTitleContainsNewline,
        context: Map<String, Any>
    ): RecoveryResult {
        val originalTitle = context["originalTitle"] as? String ?: ""
        val titleConfig = configuration.titleConfig()

        val suggestions = if (originalTitle.isNotBlank()) {
            listOf(
                titleConfig.cleanTitle(originalTitle),
                originalTitle.replace("\n", " - ")
                    .replace("\r", " - ")
                    .replace(Regex("\\s+-\\s+"), " - ")
                    .trim(),
                originalTitle.split(Regex("[\n\r]+")).first().trim() // Just take first line
            ).filter { it.isNotBlank() }
        } else {
            listOf(titleConfig.generateDefaultTitle())
        }

        return RecoveryResult.Suggestion(
            originalError = error,
            suggestedValues = suggestions,
            strategy = RecoveryStrategy.CLEAN_FORMAT,
            description = "Titles cannot contain line breaks. " +
                "Here are cleaned versions that preserve your content."
        )
    }

    /**
     * Suggests recovery for description too long validation errors.
     */
    private fun suggestDescriptionTooLongRecovery(
        error: DomainError.ScopeValidationError.ScopeDescriptionTooLong,
        context: Map<String, Any>
    ): RecoveryResult {
        val originalDescription = context["originalDescription"] as? String ?: ""
        val descriptionConfig = configuration.descriptionConfig()
        val maxLength = descriptionConfig.maxLength

        val suggestions = if (originalDescription.isNotBlank()) {
            listOf(
                descriptionConfig.truncateDescription(originalDescription),
                descriptionConfig.extractFirstSentence(originalDescription),
                originalDescription.split("\n").first().trim()  // Take first paragraph
            ).filter { it.isNotBlank() }.distinct()
        } else {
            listOf("") // Empty description is valid
        }

        return RecoveryResult.Suggestion(
            originalError = error,
            suggestedValues = suggestions,
            strategy = RecoveryStrategy.TRUNCATE,
            description = "Description exceeds maximum length (${maxLength} characters). " +
                "Here are shortened versions that preserve key information."
        )
    }

    /**
     * Suggests recovery for duplicate title business rule violations.
     */
    private fun suggestDuplicateTitleRecovery(
        error: DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle,
        context: Map<String, Any>
    ): RecoveryResult {
        val originalTitle = context["originalTitle"] as? String ?: error.title
        val parentId = context["parentId"] as? ScopeId
        val allScopes = context["allScopes"] as? List<Scope> ?: emptyList()

        val suggestions = generateUniqueVariants(originalTitle, parentId, allScopes)

        return RecoveryResult.Suggestion(
            originalError = error,
            suggestedValues = suggestions,
            strategy = RecoveryStrategy.GENERATE_VARIANTS,
            description = "A scope with this title already exists. " +
                "Here are unique variations you can use instead."
        )
    }

    /**
     * Suggests recovery for max depth exceeded business rule violations.
     */
    private fun suggestMaxDepthExceededRecovery(
        error: DomainError.ScopeBusinessRuleViolation.ScopeMaxDepthExceeded
    ): RecoveryResult {
        val hierarchyConfig = configuration.hierarchyConfig()

        return RecoveryResult.Suggestion(
            originalError = error,
            suggestedValues = listOf(
                "Move this scope to a higher level in the hierarchy",
                "Consider restructuring parent scopes to reduce nesting",
                "Create a separate top-level scope for this content"
            ),
            strategy = RecoveryStrategy.RESTRUCTURE_HIERARCHY,
            description = hierarchyConfig.getDepthGuidance(error.maxDepth, error.actualDepth)
        )
    }

    /**
     * Suggests recovery for max children exceeded business rule violations.
     */
    private fun suggestMaxChildrenExceededRecovery(
        error: DomainError.ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded
    ): RecoveryResult {
        val hierarchyConfig = configuration.hierarchyConfig()

        return RecoveryResult.Suggestion(
            originalError = error,
            suggestedValues = listOf(
                "Create intermediate grouping scopes to organize children",
                "Move some children to different parent scopes",
                "Combine related children into sub-categories"
            ),
            strategy = RecoveryStrategy.RESTRUCTURE_HIERARCHY,
            description = hierarchyConfig.getChildrenGuidance(error.maxChildren, error.actualChildren)
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
        allScopes: List<Scope>
    ): List<String> {
        val existingTitles = allScopes
            .filter { it.parentId == parentId }
            .map { it.title.value.trim().lowercase() }
            .toSet()

        val duplicationConfig = configuration.duplicationConfig()
        val variants = mutableListOf<String>()

        for (i in 1..duplicationConfig.maxRetryAttempts) {
            val variant = duplicationConfig.generateVariant(originalTitle, i)

            if (variant.trim().lowercase() !in existingTitles) {
                variants.add(variant)
                if (variants.size >= MAX_UNIQUE_VARIANTS) {
                    break // Provide up to 3 suggestions
                }
            }
        }

        return variants.ifEmpty { listOf("$originalTitle (Copy)") }
    }
}
