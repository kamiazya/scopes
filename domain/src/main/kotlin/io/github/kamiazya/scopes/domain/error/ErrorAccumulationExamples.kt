package io.github.kamiazya.scopes.domain.error

import arrow.core.nonEmptyListOf

/**
 * Examples demonstrating error accumulation features.
 *
 * This file shows practical usage patterns for the ValidationResult type and
 * its extensions, demonstrating how to accumulate validation errors instead
 * of failing fast on the first error.
 */
object ErrorAccumulationExamples {

    /**
     * Example: Validating a Scope title with multiple constraints.
     * This demonstrates collecting all validation errors instead of stopping at the first one.
     */
    fun validateScopeTitle(title: String?): ValidationResult<String> {
        return title
            .validateNotNull(DomainError.ValidationError.EmptyTitle)
            .flatMap { it.validateWith({ s -> s.trim().isNotEmpty() }, DomainError.ValidationError.EmptyTitle) }
            .flatMap { it.validateWith({ s -> s.length >= 3 }, DomainError.ValidationError.TitleTooShort) }
            .flatMap { validTitle ->
                validTitle.validateWith(
                    { s -> s.length <= 100 },
                    DomainError.ValidationError.TitleTooLong(100, validTitle.length)
                )
            }
    }

    /**
     * Example: Validating multiple fields of a Scope and collecting all errors.
     * This shows how to accumulate errors from different validation operations.
     */
    fun validateScopeFields(
        title: String?,
        description: String?
    ): ValidationResult<Pair<String, String?>> {
        val titleValidation = validateScopeTitle(title)
        val descriptionValidation = description?.let { desc ->
            desc.validateWith(
                { it.length <= 1000 },
                DomainError.ValidationError.DescriptionTooLong(1000, desc.length)
            )
        } ?: ValidationResult.Success(null)

        return titleValidation.combine(descriptionValidation) { validTitle, validDesc ->
            validTitle to validDesc
        }
    }

    /**
     * Example: Validating a list of titles and collecting all validation errors.
     * This demonstrates the traverse function for batch validation.
     */
    fun validateMultipleTitles(titles: List<String?>): ValidationResult<List<String>> {
        return titles.traverse { title -> validateScopeTitle(title) }
    }

    /**
     * Example: Creating an AggregateValidation error for form validation.
     * This shows how to organize field-specific errors for UI presentation.
     */
    fun createFormValidationError(): DomainError.AggregateValidation {
        val fieldErrors = mapOf(
            "title" to nonEmptyListOf(
                DomainError.ValidationError.EmptyTitle,
                DomainError.ValidationError.TitleTooShort
            ),
            "description" to nonEmptyListOf(
                DomainError.ValidationError.DescriptionTooLong(1000, 1500)
            ),
            "parent" to nonEmptyListOf(
                DomainError.ScopeError.ScopeNotFound
            )
        )

        return DomainError.AggregateValidation(fieldErrors)
    }

    /**
     * Example: Using the fold operation for error handling.
     * This demonstrates how to handle both success and failure cases uniformly.
     */
    fun handleValidationResult(title: String?): String {
        return validateScopeTitle(title).fold(
            ifFailure = { errors ->
                "Validation failed with ${errors.size} errors: ${errors.joinToString(", ")}"
            },
            ifSuccess = { validTitle ->
                "Valid title: '$validTitle'"
            }
        )
    }

    /**
     * Example: Using side effects for logging without affecting the validation chain.
     */
    fun validateWithLogging(title: String?): ValidationResult<String> {
        return validateScopeTitle(title)
            .onSuccess { validTitle -> println("Successfully validated title: $validTitle") }
            .onFailure { errors -> println("Validation failed with ${errors.size} errors") }
    }

    /**
     * Example: Complex validation with error recovery.
     * This shows how to provide fallback values while still collecting validation information.
     */
    fun validateTitleWithFallback(title: String?): ValidationResult<String> {
        val result = validateScopeTitle(title)
        return when {
            result.isSuccess -> result
            title?.trim()?.isNotEmpty() == true -> {
                // If only length validation failed, we might want to truncate
                val truncated = title.trim().take(100)
                if (truncated.length >= 3) {
                    ValidationResult.Success(truncated)
                } else {
                    result // Return original failure
                }
            }
            else -> result
        }
    }

    /**
     * Example: Converting between ValidationResult and Arrow's Either.
     * This demonstrates interoperability with existing Arrow-based code.
     */
    fun convertToEither(title: String?) = validateScopeTitle(title).toEither()

    /**
     * Example: Building ValidationResult from Either.
     */
    fun convertFromEither(either: arrow.core.Either<arrow.core.NonEmptyList<DomainError>, String>) =
        ValidationResult.fromEither(either)
}
