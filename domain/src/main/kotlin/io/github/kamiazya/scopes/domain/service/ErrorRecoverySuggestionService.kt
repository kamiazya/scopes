package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.RecoveryResult
import io.github.kamiazya.scopes.domain.error.RecoveryStrategy
import io.github.kamiazya.scopes.domain.error.ScopeRecoveryConfiguration
import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Sealed class representing typed context for error recovery suggestions.
 * Provides type-safe context information for each error type, replacing untyped Map<String, Any>.
 */
sealed class SuggestionContext {

    /**
     * Context for title validation errors.
     */
    data class TitleValidation(
        val originalTitle: String
    ) : SuggestionContext()

    /**
     * Context for description validation errors.
     */
    data class DescriptionValidation(
        val originalDescription: String
    ) : SuggestionContext()

    /**
     * Context for duplicate title business rule violations.
     */
    data class DuplicateTitle(
        val originalTitle: String,
        val parentId: ScopeId?,
        val allScopes: List<Scope>
    ) : SuggestionContext()

    /**
     * Context for errors that don't require additional context.
     */
    object NoContext : SuggestionContext()
}

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
        context: SuggestionContext
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

    private fun suggestTitleTooShortRecovery(
        error: DomainError.ScopeValidationError.ScopeTitleTooShort,
        context: SuggestionContext
    ): RecoveryResult {
        val originalTitle = when (context) {
            is SuggestionContext.TitleValidation -> context.originalTitle
            else -> throw IllegalArgumentException(
                "Invalid context type for title too short recovery. " +
                "Expected TitleValidation, got ${context::class.simpleName}. " +
                "This indicates a programming error in the calling code."
            )
        }
        val titleConfig = configuration.titleConfig()
        val maxLength = titleConfig.maxLength

        val suggestions = if (originalTitle.isNotBlank()) {
            val candidateSuggestions = listOf(
                "$originalTitle - Task",
                "$originalTitle - Item",
                "TODO: $originalTitle"
            )

            // Apply titleConfig normalization to enforce maxLength and remove duplicates
            val normalizedSuggestions = candidateSuggestions
                .map { candidate ->
                    // Apply titleConfig normalization to ensure maxLength compliance
                    if (candidate.length > maxLength) {
                        titleConfig.truncateTitle(candidate)
                    } else {
                        candidate
                    }
                }
                .distinct() // Remove duplicates
                .filter { it.isNotBlank() } // Filter out blanks

            if (normalizedSuggestions.isEmpty()) {
                listOf(titleConfig.generateDefaultTitle())
            } else {
                normalizedSuggestions
            }
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

    private fun suggestTitleTooLongRecovery(
        error: DomainError.ScopeValidationError.ScopeTitleTooLong,
        context: SuggestionContext
    ): RecoveryResult {
        val originalTitle = when (context) {
            is SuggestionContext.TitleValidation -> context.originalTitle
            else -> throw IllegalArgumentException(
                "Invalid context type for title too long recovery. " +
                "Expected TitleValidation, got ${context::class.simpleName}. " +
                "This indicates a programming error in the calling code."
            )
        }
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

            // Apply titleConfig normalization to enforce maxLength and remove duplicates
            val normalizedSuggestions = candidateSuggestions
                .filter { it.isNotBlank() }
                .map { candidate ->
                    // Apply titleConfig normalization to ensure maxLength compliance
                    if (candidate.length > maxLength) {
                        titleConfig.truncateTitle(candidate)
                    } else {
                        candidate
                    }
                }
                .distinct() // Remove duplicates
                .filter { it.isNotBlank() } // Filter again in case truncation created blanks

            if (normalizedSuggestions.isEmpty()) {
                listOf(titleConfig.generateDefaultTitle())
            } else {
                normalizedSuggestions
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

    private fun suggestTitleContainsNewlineRecovery(
        error: DomainError.ScopeValidationError.ScopeTitleContainsNewline,
        context: SuggestionContext
    ): RecoveryResult {
        val originalTitle = when (context) {
            is SuggestionContext.TitleValidation -> context.originalTitle
            else -> throw IllegalArgumentException(
                "Invalid context type for title contains newline recovery. " +
                "Expected TitleValidation, got ${context::class.simpleName}. " +
                "This indicates a programming error in the calling code."
            )
        }
        val titleConfig = configuration.titleConfig()
        val maxLength = titleConfig.maxLength

        val suggestions = if (originalTitle.isNotBlank()) {
            val candidateSuggestions = listOf(
                titleConfig.cleanTitle(originalTitle),
                originalTitle.replace("\n", " - ")
                    .replace("\r", " - ")
                    .replace(Regex("\\s+-\\s+"), " - ")
                    .trim(),
                originalTitle.split(Regex("[\n\r]+")).first().trim() // Just take first line
            )

            // Apply titleConfig normalization to enforce maxLength and remove duplicates
            val normalizedSuggestions = candidateSuggestions
                .filter { it.isNotBlank() }
                .map { candidate ->
                    // Apply titleConfig normalization to ensure maxLength compliance
                    if (candidate.length > maxLength) {
                        titleConfig.truncateTitle(candidate)
                    } else {
                        candidate
                    }
                }
                .distinct() // Remove duplicates
                .filter { it.isNotBlank() } // Filter again in case truncation created blanks

            if (normalizedSuggestions.isEmpty()) {
                listOf(titleConfig.generateDefaultTitle())
            } else {
                normalizedSuggestions
            }
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

    private fun suggestDescriptionTooLongRecovery(
        error: DomainError.ScopeValidationError.ScopeDescriptionTooLong,
        context: SuggestionContext
    ): RecoveryResult {
        val originalDescription = when (context) {
            is SuggestionContext.DescriptionValidation -> context.originalDescription
            else -> throw IllegalArgumentException(
                "Invalid context type for description too long recovery. " +
                "Expected DescriptionValidation, got ${context::class.simpleName}. " +
                "This indicates a programming error in the calling code."
            )
        }
        val descriptionConfig = configuration.descriptionConfig()
        val maxLength = descriptionConfig.maxLength

        val suggestions = if (originalDescription.isNotBlank()) {
            val candidateSuggestions = listOf(
                descriptionConfig.truncateDescription(originalDescription),
                descriptionConfig.extractFirstSentence(originalDescription),
                originalDescription.split("\n").first().trim()  // Take first paragraph
            )

            // Apply descriptionConfig normalization to enforce maxLength and remove duplicates
            val normalizedSuggestions = candidateSuggestions
                .filter { it.isNotBlank() }
                .map { candidate ->
                    // Apply descriptionConfig normalization to ensure maxLength compliance
                    if (candidate.length > maxLength) {
                        descriptionConfig.truncateDescription(candidate)
                    } else {
                        candidate
                    }
                }
                .distinct() // Remove duplicates
                .filter { it.isNotBlank() } // Filter again in case truncation created blanks

            if (normalizedSuggestions.isEmpty()) {
                listOf("") // Empty description is valid fallback
            } else {
                normalizedSuggestions
            }
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

    private fun suggestDuplicateTitleRecovery(
        error: DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle,
        context: SuggestionContext
    ): RecoveryResult {
        val (originalTitle, parentId, allScopes) = when (context) {
            is SuggestionContext.DuplicateTitle -> {
                Triple(context.originalTitle, context.parentId, context.allScopes)
            }
            else -> throw IllegalArgumentException(
                "Invalid context type for duplicate title recovery. " +
                "Expected DuplicateTitle, got ${context::class.simpleName}. " +
                "This indicates a programming error in the calling code."
            )
        }

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
     * Applies max length constraints and removes duplicates after normalization.
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
        val titleConfig = configuration.titleConfig()
        val maxLength = titleConfig.maxLength
        val variants = mutableListOf<String>()
        val normalizedVariants = mutableSetOf<String>()

        for (i in 1..duplicationConfig.maxRetryAttempts) {
            val variant = duplicationConfig.generateVariant(originalTitle, i)

            // Apply max length constraint by truncating if necessary
            val constrainedVariant = if (variant.length > maxLength) {
                titleConfig.truncateTitle(variant)
            } else {
                variant
            }

            val normalizedVariant = constrainedVariant.trim().lowercase()

            // Check if this variant is unique (not in existing titles and not already added)
            if (normalizedVariant !in existingTitles && normalizedVariant !in normalizedVariants) {
                variants.add(constrainedVariant)
                normalizedVariants.add(normalizedVariant)
                if (variants.size >= MAX_UNIQUE_VARIANTS) {
                    break // Provide up to 3 suggestions
                }
            }
        }

        // Fallback with length constraint applied
        val fallback = "$originalTitle (Copy)"
        val constrainedFallback = if (fallback.length > maxLength) {
            titleConfig.truncateTitle(fallback)
        } else {
            fallback
        }

        return variants.ifEmpty { listOf(constrainedFallback) }
    }
}
