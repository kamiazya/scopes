package io.github.kamiazya.scopes.application.error

import io.github.kamiazya.scopes.domain.error.*
import io.github.kamiazya.scopes.domain.service.ErrorRecoveryDomainService
import io.github.kamiazya.scopes.domain.service.RecoveryStrategyDomainService
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

    "ErrorRecoveryApplicationService should exist with proper dependencies" {
        // Test setup
        val errorCategorizationService = ErrorRecoveryDomainService()
        val recoveryStrategyService = RecoveryStrategyDomainService()
        
        val applicationService = ErrorRecoveryApplicationService(
            errorCategorizationService = errorCategorizationService,
            recoveryStrategyService = recoveryStrategyService
        )
        
        // Should be able to create the application service
        applicationService.shouldBeInstanceOf<ErrorRecoveryApplicationService>()
    }

    "recoverFromError should orchestrate complete error recovery process" {
        // Test setup
        val errorCategorizationService = ErrorRecoveryDomainService()
        val recoveryStrategyService = RecoveryStrategyDomainService()
        val applicationService = ErrorRecoveryApplicationService(
            errorCategorizationService = errorCategorizationService,
            recoveryStrategyService = recoveryStrategyService
        )
        
        val error = DomainError.ScopeValidationError.EmptyScopeTitle
        val context = mapOf("originalTitle" to "Test Title")
        
        val result = applicationService.recoverFromError(error, context)
        
        // Should orchestrate between domain assessment and suggestion generation
        result.shouldNotBeNull()
        result.shouldBeInstanceOf<RecoveryResult.Suggestion>()
    }

    "recoverFromError should delegate to domain service for categorization" {
        // Test setup
        val errorCategorizationService = ErrorRecoveryDomainService()
        val recoveryStrategyService = RecoveryStrategyDomainService()
        val applicationService = ErrorRecoveryApplicationService(
            errorCategorizationService = errorCategorizationService,
            recoveryStrategyService = recoveryStrategyService
        )
        
        val recoverableError = DomainError.ScopeValidationError.EmptyScopeTitle
        val nonRecoverableError = DomainError.ScopeError.ScopeNotFound
        
        val recoverableResult = applicationService.recoverFromError(recoverableError, emptyMap())
        val nonRecoverableResult = applicationService.recoverFromError(nonRecoverableError, emptyMap())
        
        // Should use domain service categorization to determine response
        recoverableResult.shouldBeInstanceOf<RecoveryResult.Suggestion>()
        nonRecoverableResult.shouldBeInstanceOf<RecoveryResult.NonRecoverable>()
    }

    "recoverFromValidationResult should handle ValidationResult workflows" {
        // Test setup
        val errorCategorizationService = ErrorRecoveryDomainService()
        val recoveryStrategyService = RecoveryStrategyDomainService()
        val applicationService = ErrorRecoveryApplicationService(
            errorCategorizationService = errorCategorizationService,
            recoveryStrategyService = recoveryStrategyService
        )
        
        val failureResult = ValidationResult.Failure<String>(
            nonEmptyListOf(
                DomainError.ScopeValidationError.EmptyScopeTitle,
                DomainError.ScopeValidationError.ScopeTitleTooShort
            )
        )
        val context = mapOf("originalTitle" to "Test")
        
        val recoveredResult = applicationService.recoverFromValidationResult(failureResult, context)
        
        // Should orchestrate recovery for each error in ValidationResult
        recoveredResult.shouldBeInstanceOf<RecoveredValidationResult<String>>()
        recoveredResult.originalResult shouldBe failureResult
        recoveredResult.recoveryResults shouldHaveSize 2
        recoveredResult.recoveryResults.all { it is RecoveryResult.Suggestion } shouldBe true
    }

    "recoverFromValidationResult should handle success cases gracefully" {
        // Test setup
        val errorCategorizationService = ErrorRecoveryDomainService()
        val recoveryStrategyService = RecoveryStrategyDomainService()
        val applicationService = ErrorRecoveryApplicationService(
            errorCategorizationService = errorCategorizationService,
            recoveryStrategyService = recoveryStrategyService
        )
        
        val successResult = ValidationResult.Success("Valid Value")
        val context = emptyMap<String, Any>()
        
        val recoveredResult = applicationService.recoverFromValidationResult(successResult, context)
        
        // Should handle success without attempting recovery
        recoveredResult.shouldBeInstanceOf<RecoveredValidationResult<String>>()
        recoveredResult.originalResult shouldBe successResult
        recoveredResult.recoveryResults shouldHaveSize 0
    }

    "application service should coordinate but not contain domain logic" {
        // Test setup
        val errorCategorizationService = ErrorRecoveryDomainService()
        val recoveryStrategyService = RecoveryStrategyDomainService()
        val applicationService = ErrorRecoveryApplicationService(
            errorCategorizationService = errorCategorizationService,
            recoveryStrategyService = recoveryStrategyService
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

    "application service should use infrastructure formatting for messages" {
        // Test setup
        val errorCategorizationService = ErrorRecoveryDomainService()
        val recoveryStrategyService = RecoveryStrategyDomainService()
        val applicationService = ErrorRecoveryApplicationService(
            errorCategorizationService = errorCategorizationService,
            recoveryStrategyService = recoveryStrategyService
        )
        
        val error = DomainError.ScopeValidationError.EmptyScopeTitle
        val result = applicationService.recoverFromError(error, emptyMap())
        
        // Should use infrastructure formatting service for message generation
        result.shouldBeInstanceOf<RecoveryResult.Suggestion>()
        val suggestion = result as RecoveryResult.Suggestion
        suggestion.description.shouldNotBeNull()
        // The description should be formatted using ErrorFormattingUtils logic
        suggestion.description shouldContain "title"
    }
})
