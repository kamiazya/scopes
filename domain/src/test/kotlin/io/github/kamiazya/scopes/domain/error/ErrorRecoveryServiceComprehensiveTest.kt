package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.string.shouldContain
import arrow.core.nonEmptyListOf

/**
 * Comprehensive test to verify the complete transformation to suggestion-only behavior.
 */
class ErrorRecoveryServiceComprehensiveTest : StringSpec({

    val service = ErrorRecoveryService()

    "all validation errors should return meaningful suggestions" {
        val testCases = mapOf(
            DomainError.ValidationError.EmptyTitle to "default",
            DomainError.ValidationError.TitleTooShort to "short",
            DomainError.ValidationError.TitleTooLong(maxLength = 200, actualLength = 300) to "length",
            DomainError.ValidationError.TitleContainsNewline to "line break",
            DomainError.ValidationError.DescriptionTooLong(maxLength = 1000, actualLength = 2000) to "description"
        )

        testCases.forEach { (error, expectedDescriptionKeyword) ->
            val result = service.recoverFromError(error, mapOf("originalTitle" to "Test Title"))

            result.shouldBeInstanceOf<RecoveryResult.Suggestion>()
            result.suggestedValues.shouldNotBeEmpty()
            result.description.shouldNotBeBlank()
            // Verify the description is relevant to the error type
            result.description.lowercase() shouldContain expectedDescriptionKeyword
        }
    }

    "error categorization should have no RECOVERABLE category" {
        val categories = ErrorRecoveryCategory.values()
        categories shouldBe arrayOf(
            ErrorRecoveryCategory.PARTIALLY_RECOVERABLE,
            ErrorRecoveryCategory.NON_RECOVERABLE
        )
    }

    "all validation errors should be categorized as PARTIALLY_RECOVERABLE" {
        val validationErrors = listOf(
            DomainError.ValidationError.EmptyTitle,
            DomainError.ValidationError.TitleTooShort,
            DomainError.ValidationError.TitleTooLong(200, 300),
            DomainError.ValidationError.TitleContainsNewline,
            DomainError.ValidationError.DescriptionTooLong(1000, 2000)
        )

        validationErrors.forEach { error ->
            val category = service.categorizeError(error)
            category shouldBe ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
        }
    }

    "configuration should not have enableAutoRecovery field" {
        val config = RecoveryConfiguration()
        val fields = RecoveryConfiguration::class.java.declaredFields
        val autoRecoveryField = fields.find { it.name == "enableAutoRecovery" }

        autoRecoveryField shouldBe null
    }

    "ValidationResult integration should never produce Success results" {
        val failureResult = ValidationResult.Failure<String>(
            nonEmptyListOf(
                DomainError.ValidationError.EmptyTitle,
                DomainError.ValidationError.TitleTooShort,
                DomainError.ValidationError.TitleTooLong(200, 300)
            )
        )

        val recoveredResult = service.recoverFromValidationResult(failureResult)

        // Should have no automatic recoveries (suggestion-only system)

        // Should have suggestions instead
        recoveredResult.hasAnySuggestions() shouldBe true

        // All results should be suggestions
        recoveredResult.recoveryResults.forEach { result ->
            result.shouldBeInstanceOf<RecoveryResult.Suggestion>()
        }
    }

    "recovery extensions should work with suggestion-only system" {
        val failureResult = ValidationResult.Failure<String>(
            nonEmptyListOf(DomainError.ValidationError.EmptyTitle)
        )

        val recoveredResult = failureResult.withRecovery()
        val summary = recoveredResult.getRecoverySummary()

        // Should reflect suggestion-only behavior
        summary.requiresUserInput shouldBe 1
        summary.cannotRecover shouldBe 0

        // Summary should mention suggestions
        summary.toReadableString() shouldContain "suggestions"
    }

    "context-aware suggestions should preserve user input" {
        val originalTitle = "My Important Task"
        val context = mapOf("originalTitle" to originalTitle)

        val result = service.recoverFromError(
            DomainError.ValidationError.TitleTooShort,
            context
        ) as RecoveryResult.Suggestion

        // Should incorporate original title in suggestions
        result.suggestedValues.any { suggestion ->
            suggestion.toString().contains(originalTitle)
        } shouldBe true
    }
})
