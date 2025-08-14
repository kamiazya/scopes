package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.service.ErrorRecoveryDomainService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Test for ErrorRecoveryDomainService.
 */
class ErrorRecoveryDomainServiceTest : StringSpec({

    "ErrorRecoveryDomainService should exist with proper domain dependencies" {
        // Test setup
        val service = ErrorRecoveryDomainService()

        // Should be able to create the service
        service.shouldBeInstanceOf<ErrorRecoveryDomainService>()
    }

    "categorizeError should categorize validation errors as PARTIALLY_RECOVERABLE" {
        // Test setup
        val service = ErrorRecoveryDomainService()

        val emptyTitleError = ScopeValidationError.EmptyScopeTitle
        val tooShortError = ScopeValidationError.ScopeTitleTooShort
        val tooLongError = ScopeValidationError.ScopeTitleTooLong(200, 300)
        val newlineError = ScopeValidationError.ScopeTitleContainsNewline
        val descTooLongError = ScopeValidationError.ScopeDescriptionTooLong(1000, 1500)

        service.categorizeError(emptyTitleError) shouldBe ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
        service.categorizeError(tooShortError) shouldBe ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
        service.categorizeError(tooLongError) shouldBe ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
        service.categorizeError(newlineError) shouldBe ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
        service.categorizeError(descTooLongError) shouldBe ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
    }

    "categorizeError should categorize invalid format validation errors as NON_RECOVERABLE" {
        // Test setup
        val service = ErrorRecoveryDomainService()

        val invalidFormatError = ScopeValidationError.ScopeInvalidFormat("title", "String")

        service.categorizeError(invalidFormatError) shouldBe ErrorRecoveryCategory.NON_RECOVERABLE
    }

    "categorizeError should categorize business rule violations as PARTIALLY_RECOVERABLE" {
        // Test setup
        val service = ErrorRecoveryDomainService()

        val duplicateError = ScopeBusinessRuleViolation.ScopeDuplicateTitle("Task", null)
        val maxDepthError = ScopeBusinessRuleViolation.ScopeMaxDepthExceeded(5, 8)
        val maxChildrenError = ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded(10, 15)

        service.categorizeError(duplicateError) shouldBe ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
        service.categorizeError(maxDepthError) shouldBe ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
        service.categorizeError(maxChildrenError) shouldBe ErrorRecoveryCategory.PARTIALLY_RECOVERABLE
    }

    "categorizeError should categorize scope errors as NON_RECOVERABLE" {
        // Test setup
        val service = ErrorRecoveryDomainService()

        val scopeNotFoundError = ScopeError.ScopeNotFound

        service.categorizeError(scopeNotFoundError) shouldBe ErrorRecoveryCategory.NON_RECOVERABLE
    }

    "isRecoverable should return true for PARTIALLY_RECOVERABLE errors" {
        // Test setup
        val service = ErrorRecoveryDomainService()

        val emptyTitleError = ScopeValidationError.EmptyScopeTitle
        val duplicateError = ScopeBusinessRuleViolation.ScopeDuplicateTitle("Task", null)

        service.isRecoverable(emptyTitleError) shouldBe true
        service.isRecoverable(duplicateError) shouldBe true
    }

    "isRecoverable should return false for NON_RECOVERABLE errors" {
        // Test setup
        val service = ErrorRecoveryDomainService()

        val scopeNotFoundError = ScopeError.ScopeNotFound
        val invalidFormatError = ScopeValidationError.ScopeInvalidFormat("title", "String")

        service.isRecoverable(scopeNotFoundError) shouldBe false
        service.isRecoverable(invalidFormatError) shouldBe false
    }

    "getRecoveryComplexity should assess complexity from domain perspective" {
        // Test setup
        val service = ErrorRecoveryDomainService()

        // Simple errors - direct suggestions possible
        val emptyTitleError = ScopeValidationError.EmptyScopeTitle
        val tooShortError = ScopeValidationError.ScopeTitleTooShort

        service.getRecoveryComplexity(emptyTitleError) shouldBe RecoveryComplexity.SIMPLE
        service.getRecoveryComplexity(tooShortError) shouldBe RecoveryComplexity.SIMPLE

        // Moderate errors - require user choices
        val tooLongError = ScopeValidationError.ScopeTitleTooLong(200, 300)
        val duplicateError = ScopeBusinessRuleViolation.ScopeDuplicateTitle("Task", null)

        service.getRecoveryComplexity(tooLongError) shouldBe RecoveryComplexity.MODERATE
        service.getRecoveryComplexity(duplicateError) shouldBe RecoveryComplexity.MODERATE

        // Complex errors - significant intervention needed
        val maxDepthError = ScopeBusinessRuleViolation.ScopeMaxDepthExceeded(5, 8)
        val maxChildrenError = ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded(10, 15)

        service.getRecoveryComplexity(maxDepthError) shouldBe RecoveryComplexity.COMPLEX
        service.getRecoveryComplexity(maxChildrenError) shouldBe RecoveryComplexity.COMPLEX
    }

    "domain service should be pure and stateless" {
        // Test setup
        val service1 = ErrorRecoveryDomainService()
        val service2 = ErrorRecoveryDomainService()

        val error = ScopeValidationError.EmptyScopeTitle

        // Same configuration should produce identical results (pure functions)
        service1.categorizeError(error) shouldBe service2.categorizeError(error)
        service1.isRecoverable(error) shouldBe service2.isRecoverable(error)
        service1.getRecoveryComplexity(error) shouldBe service2.getRecoveryComplexity(error)
    }

    "domain service should not contain application orchestration logic" {
        // Test setup
        val service = ErrorRecoveryDomainService()

        // Domain service should only have pure categorization methods, no orchestration
        val methods = service::class.java.declaredMethods.map { it.name }

        // Should have domain methods
        methods shouldContain "categorizeError"
        methods shouldContain "isRecoverable"
        methods shouldContain "getRecoveryComplexity"

        // Should NOT have application orchestration methods
        val forbiddenMethods = listOf("recoverFromError", "recoverFromErrors", "recoverFromValidationResult")
        forbiddenMethods.forEach { method ->
            methods shouldNotContain method
        }
    }
})
