package io.github.kamiazya.scopes.application.error

import io.github.kamiazya.scopes.domain.error.*
import io.github.kamiazya.scopes.domain.service.ErrorRecoveryDomainService
import io.github.kamiazya.scopes.domain.service.RecoveryStrategyDomainService
import io.github.kamiazya.scopes.domain.service.SuggestionContext
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain

/**
 * Test for ErrorRecoveryApplicationService.
 */
class ErrorRecoveryApplicationServiceTest : StringSpec({

    // Simple fake implementation of ErrorFormatter for testing
    class FakeErrorFormatter(private val messagePrefix: String = "Formatted: ") : ErrorFormatter {
        var getErrorMessageCallCount = 0
        var lastErrorReceived: DomainError? = null

        override fun getErrorMessage(error: DomainError): String {
            getErrorMessageCallCount++
            lastErrorReceived = error
            return "$messagePrefix$error"
        }

        override fun formatErrorSummary(errors: NonEmptyList<DomainError>): String {
            return "Summary of ${errors.size} errors"
        }

        override fun getValidationErrorMessage(error: ScopeValidationError): String {
            return "Validation: $error"
        }

        override fun getBusinessRuleViolationMessage(error: ScopeBusinessRuleViolation): String {
            return "Business rule: $error"
        }

        override fun getRepositoryErrorMessage(error: RepositoryError): String {
            return "Repository: $error"
        }
    }

    "ErrorRecoveryApplicationService should exist with proper dependencies" {
        // Test setup with mock formatter
        val errorCategorizationService = ErrorRecoveryDomainService()
        val recoveryStrategyService = RecoveryStrategyDomainService()
        val fakeErrorFormatter = FakeErrorFormatter()

        val applicationService = ErrorRecoveryApplicationService(
            errorCategorizationService = errorCategorizationService,
            recoveryStrategyService = recoveryStrategyService,
            errorFormatter = fakeErrorFormatter
        )

        // Should be able to create the application service
        applicationService.shouldBeInstanceOf<ErrorRecoveryApplicationService>()
    }

    "recoverFromError should orchestrate complete error recovery process" {
        // Test setup with mock formatter
        val errorCategorizationService = ErrorRecoveryDomainService()
        val recoveryStrategyService = RecoveryStrategyDomainService()
        val fakeErrorFormatter = FakeErrorFormatter()
        val applicationService = ErrorRecoveryApplicationService(
            errorCategorizationService = errorCategorizationService,
            recoveryStrategyService = recoveryStrategyService,
            errorFormatter = fakeErrorFormatter
        )

        val error = ScopeValidationError.EmptyScopeTitle
        val context = SuggestionContext.TitleValidation("Test Title")

        val result = applicationService.recoverFromError(error, context)

        // Should orchestrate between domain assessment and suggestion generation
        result.shouldBeInstanceOf<RecoveryResult.Suggestion>()
    }

    "recoverFromError should delegate to domain service for categorization" {
        // Test setup with mock formatter
        val errorCategorizationService = ErrorRecoveryDomainService()
        val recoveryStrategyService = RecoveryStrategyDomainService()
        val fakeErrorFormatter = FakeErrorFormatter()
        val applicationService = ErrorRecoveryApplicationService(
            errorCategorizationService = errorCategorizationService,
            recoveryStrategyService = recoveryStrategyService,
            errorFormatter = fakeErrorFormatter
        )

        val recoverableError = ScopeValidationError.EmptyScopeTitle
        val nonRecoverableError = ScopeError.ScopeNotFound

        val recoverableResult = applicationService.recoverFromError(recoverableError)
        val nonRecoverableResult = applicationService.recoverFromError(nonRecoverableError)

        // Should use domain service categorization to determine response
        recoverableResult.shouldBeInstanceOf<RecoveryResult.Suggestion>()
        nonRecoverableResult.shouldBeInstanceOf<RecoveryResult.NonRecoverable>()
    }

    "recoverFromValidationResult should handle ValidationResult workflows" {
        // Test setup with mock formatter
        val errorCategorizationService = ErrorRecoveryDomainService()
        val recoveryStrategyService = RecoveryStrategyDomainService()
        val fakeErrorFormatter = FakeErrorFormatter()
        val applicationService = ErrorRecoveryApplicationService(
            errorCategorizationService = errorCategorizationService,
            recoveryStrategyService = recoveryStrategyService,
            errorFormatter = fakeErrorFormatter
        )

        val failureResult = ValidationResult.Failure(
            nonEmptyListOf(
                ScopeValidationError.EmptyScopeTitle,
                ScopeValidationError.ScopeTitleTooShort
            )
        )
        val context = SuggestionContext.TitleValidation("Test")

        val recoveredResult = applicationService.recoverFromValidationResult(failureResult, context)

        // Should orchestrate recovery for each error in ValidationResult
        recoveredResult.shouldBeInstanceOf<RecoveredValidationResult<String>>()
        recoveredResult.originalResult shouldBe failureResult
        recoveredResult.recoveryResults shouldHaveSize 2
        recoveredResult.recoveryResults.all { it is RecoveryResult.Suggestion } shouldBe true
    }

    "recoverFromValidationResult should handle success cases gracefully" {
        // Test setup with mock formatter
        val errorCategorizationService = ErrorRecoveryDomainService()
        val recoveryStrategyService = RecoveryStrategyDomainService()
        val fakeErrorFormatter = FakeErrorFormatter()
        val applicationService = ErrorRecoveryApplicationService(
            errorCategorizationService = errorCategorizationService,
            recoveryStrategyService = recoveryStrategyService,
            errorFormatter = fakeErrorFormatter
        )

        val successResult = ValidationResult.Success("Valid Value")

        val recoveredResult = applicationService.recoverFromValidationResult(successResult)

        // Should handle success without attempting recovery
        recoveredResult.shouldBeInstanceOf<RecoveredValidationResult<String>>()
        recoveredResult.originalResult shouldBe successResult
        recoveredResult.recoveryResults shouldHaveSize 0
    }

    "application service should coordinate but not contain domain logic" {
        // Test setup with mock formatter
        val errorCategorizationService = ErrorRecoveryDomainService()
        val recoveryStrategyService = RecoveryStrategyDomainService()
        val fakeErrorFormatter = FakeErrorFormatter()
        val applicationService = ErrorRecoveryApplicationService(
            errorCategorizationService = errorCategorizationService,
            recoveryStrategyService = recoveryStrategyService,
            errorFormatter = fakeErrorFormatter
        )

        val methods = applicationService::class.java.declaredMethods.map { it.name }

        // Should have orchestration methods
        methods shouldContain "recoverFromError"
        methods shouldContain "recoverFromValidationResult"

        // Should NOT have domain logic methods (those belong in domain service)
        val forbiddenDomainMethods = listOf("categorizeError", "isRecoverable", "getRecoveryComplexity")
        forbiddenDomainMethods.forEach { method ->
            methods shouldNotContain method
        }
    }

    "application service should use injected formatter for non-recoverable errors" {
        // Test setup with fake formatter
        val errorCategorizationService = ErrorRecoveryDomainService()
        val recoveryStrategyService = RecoveryStrategyDomainService()
        val fakeErrorFormatter = FakeErrorFormatter("Test: ")

        val nonRecoverableError = ScopeError.ScopeNotFound

        val applicationService = ErrorRecoveryApplicationService(
            errorCategorizationService = errorCategorizationService,
            recoveryStrategyService = recoveryStrategyService,
            errorFormatter = fakeErrorFormatter
        )

        val result = applicationService.recoverFromError(nonRecoverableError)

        // Should use the injected formatter for non-recoverable errors
        result.shouldBeInstanceOf<RecoveryResult.NonRecoverable>()
        val nonRecoverable = result as RecoveryResult.NonRecoverable
        nonRecoverable.reason shouldContain "Test: "

        // Verify the formatter was called exactly once
        fakeErrorFormatter.getErrorMessageCallCount shouldBe 1
        fakeErrorFormatter.lastErrorReceived shouldBe nonRecoverableError
    }
})
