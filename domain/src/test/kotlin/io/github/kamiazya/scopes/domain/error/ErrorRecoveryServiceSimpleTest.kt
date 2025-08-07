package io.github.kamiazya.scopes.domain.error

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.shouldBe

/**
 * Simple test to verify the ErrorRecoveryService transformation to suggestion-only behavior.
 * This test should FAIL initially during the RED phase.
 */
class ErrorRecoveryServiceSimpleTest : StringSpec({

    val service = ErrorRecoveryService()

    "should only return suggestions or non-recoverable results" {
        // Test validation errors to ensure they only return appropriate results
        val validationErrors = listOf(
            DomainError.ValidationError.EmptyTitle,
            DomainError.ValidationError.TitleTooShort,
            DomainError.ValidationError.TitleTooLong(maxLength = 200, actualLength = 300),
            DomainError.ValidationError.TitleContainsNewline,
            DomainError.ValidationError.DescriptionTooLong(maxLength = 1000, actualLength = 2000)
        )

        validationErrors.forEach { error ->
            val result = service.recoverFromError(error)

            // Should only return Suggestion or NonRecoverable results
            // Success results are no longer possible in suggestion-only system
            (result is RecoveryResult.Suggestion || result is RecoveryResult.NonRecoverable) shouldBe true
        }
    }
})
