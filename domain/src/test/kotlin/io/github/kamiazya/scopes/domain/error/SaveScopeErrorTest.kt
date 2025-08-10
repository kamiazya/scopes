package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Test cases for SaveScopeError sealed class.
 * These tests define the expected behavior for save operation failures.
 */
class SaveScopeErrorTest : FunSpec({
    
    context("SaveScopeError sealed class") {
        
        test("should create DuplicateId error with scope ID") {
            val scopeId = ScopeId.generate()
            val error = SaveScopeError.DuplicateId(scopeId)
            
            error.shouldBeInstanceOf<SaveScopeError.DuplicateId>()
            error.scopeId shouldBe scopeId
        }
        
        test("should create OptimisticLockFailure error with scope ID and message") {
            val scopeId = ScopeId.generate()
            val message = "Scope was modified by another process"
            val error = SaveScopeError.OptimisticLockFailure(scopeId, message)
            
            error.shouldBeInstanceOf<SaveScopeError.OptimisticLockFailure>()
            error.scopeId shouldBe scopeId
            error.message shouldBe message
        }
        
        test("should create ValidationFailure error with scope ID and validation message") {
            val scopeId = ScopeId.generate()
            val validationMessage = "Invalid scope data"
            val error = SaveScopeError.ValidationFailure(scopeId, validationMessage)
            
            error.shouldBeInstanceOf<SaveScopeError.ValidationFailure>()
            error.scopeId shouldBe scopeId
            error.message shouldBe validationMessage
        }
        
        test("should create PersistenceFailure error with scope ID, message and cause") {
            val scopeId = ScopeId.generate()
            val message = "Database connection failed"
            val cause = RuntimeException("Connection timeout")
            val error = SaveScopeError.PersistenceFailure(scopeId, message, cause)
            
            error.shouldBeInstanceOf<SaveScopeError.PersistenceFailure>()
            error.scopeId shouldBe scopeId
            error.message shouldBe message
            error.cause shouldBe cause
        }
        
        test("should create UnknownError error with scope ID, message and cause") {
            val scopeId = ScopeId.generate()
            val message = "Unexpected error occurred"
            val cause = RuntimeException("Unknown failure")
            val error = SaveScopeError.UnknownError(scopeId, message, cause)
            
            error.shouldBeInstanceOf<SaveScopeError.UnknownError>()
            error.scopeId shouldBe scopeId
            error.message shouldBe message
            error.cause shouldBe cause
        }
    }
    
    context("SaveScopeError sealed hierarchy") {
        
        test("all error types should be instances of SaveScopeError") {
            val scopeId = ScopeId.generate()
            val cause = RuntimeException("Test")
            
            val errors = listOf(
                SaveScopeError.DuplicateId(scopeId),
                SaveScopeError.OptimisticLockFailure(scopeId, "test"),
                SaveScopeError.ValidationFailure(scopeId, "test"),
                SaveScopeError.PersistenceFailure(scopeId, "test", cause),
                SaveScopeError.UnknownError(scopeId, "test", cause)
            )
            
            errors.forEach { error ->
                error.shouldBeInstanceOf<SaveScopeError>()
            }
        }
    }
})