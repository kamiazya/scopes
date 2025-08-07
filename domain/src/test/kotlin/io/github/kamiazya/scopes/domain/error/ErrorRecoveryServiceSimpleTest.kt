package io.github.kamiazya.scopes.domain.error

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldNotBeInstanceOf

/**
 * Simple test to verify the ErrorRecoveryService transformation to suggestion-only behavior.
 * This test should FAIL initially during the RED phase.
 */
class ErrorRecoveryServiceSimpleTest : StringSpec({

    val service = ErrorRecoveryService()

    "should NEVER return RecoveryResult.Success - only suggestions or non-recoverable" {
        // Test the key validation errors that currently return Success but should return Suggestions
        val validationErrors = listOf(
            DomainError.ValidationError.EmptyTitle,
            DomainError.ValidationError.TitleTooShort,
            DomainError.ValidationError.TitleTooLong(maxLength = 200, actualLength = 300),
            DomainError.ValidationError.TitleContainsNewline,
            DomainError.ValidationError.DescriptionTooLong(maxLength = 1000, actualLength = 2000)
        )

        validationErrors.forEach { error ->
            val result = service.recoverFromError(error)

            // This is the KEY ASSERTION - should fail in RED phase
            // Current implementation returns Success for these errors, but we want only Suggestions
            result.shouldNotBeInstanceOf<RecoveryResult.Success>()
        }
    }
})
