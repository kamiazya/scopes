package io.github.kamiazya.scopes.infrastructure.functional

import io.github.kamiazya.scopes.domain.error.ValidationResult
import io.github.kamiazya.scopes.domain.error.DomainError
import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Test for ValidationResultBridge.
 */
class ValidationResultBridgeTest : StringSpec({

    "ValidationResultBridge should exist in infrastructure layer" {
        // Test setup
        val bridge = ValidationResultBridge
        
        // Should be able to access the object
        bridge shouldBe ValidationResultBridge
    }

    "sequenceToValidationResult should accumulate all errors from Either list" {
        // Test setup
        val error1 = DomainError.ScopeValidationError.EmptyScopeTitle
        val error2 = DomainError.ScopeValidationError.ScopeTitleTooShort
        
        val eitherList = listOf(
            Either.Left(nonEmptyListOf(error1)),
            Either.Left(nonEmptyListOf(error2)),
            Either.Right("success")
        )
        
        val result = ValidationResultBridge.sequenceToValidationResult(eitherList)
        
        result.shouldBeInstanceOf<ValidationResult.Failure<List<String>>>()
        val failure = result as ValidationResult.Failure<List<String>>
        failure.errors.size shouldBe 2
    }

    "sequenceToEither should convert ValidationResult list to Either" {
        // Test setup
        val success1 = ValidationResult.Success("value1")
        val success2 = ValidationResult.Success("value2")
        val failure = ValidationResult.Failure<String>(nonEmptyListOf(DomainError.ScopeValidationError.EmptyScopeTitle))
        
        val validationList = listOf(success1, failure, success2)
        
        val result = ValidationResultBridge.sequenceToEither(validationList)
        
        result.shouldBeInstanceOf<Either.Left<NonEmptyList<DomainError>>>()
    }

    "recover should apply Either-based recovery function" {
        // Test setup
        val failure = ValidationResult.Failure<String>(
            nonEmptyListOf(DomainError.ScopeValidationError.EmptyScopeTitle)
        )
        
        val recoveryResult = ValidationResultBridge.recover(failure) { _ ->
            Either.Right("recovered value")  
        }
        
        recoveryResult.shouldBeInstanceOf<ValidationResult.Success<String>>()
        val success = recoveryResult as ValidationResult.Success<String>
        success.value shouldBe "recovered value"
    }

    "batchProcess should process all items with ValidationResult operations" {
        // Test setup
        val items = listOf("a", "b", "c")
        val processor: (String) -> ValidationResult<Int> = { str ->
            ValidationResult.Success(str.length)
        }
        
        val result = ValidationResultBridge.batchProcess(items, processor)
        
        result.shouldBeInstanceOf<ValidationResult.Success<List<Int>>>()
        val success = result as ValidationResult.Success<List<Int>>
        success.value shouldBe listOf(1, 1, 1)
    }

    "toValidationResultNel should wrap single Error in NonEmptyList" {
        // Test setup
        val error = DomainError.ScopeValidationError.EmptyScopeTitle
        val either = Either.Left(error)
        
        val result = ValidationResultBridge.toValidationResultNel(either)
        
        result.shouldBeInstanceOf<ValidationResult.Failure<String>>()
        val failure = result as ValidationResult.Failure<String>
        failure.errors.size shouldBe 1
        failure.errors.head shouldBe error
    }
})
