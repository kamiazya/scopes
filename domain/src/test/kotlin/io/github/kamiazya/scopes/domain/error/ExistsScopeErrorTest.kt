package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Test cases for ExistsScopeError sealed class following TDD methodology.
 * These tests define the expected behavior for existence check failures.
 */
class ExistsScopeErrorTest : FunSpec({
    
    context("ExistsScopeError sealed class") {
        
        test("should create QueryTimeout error with scopeId and timeout") {
            val scopeId = ScopeId.generate()
            val timeoutMs = 5000L
            val error = ExistsScopeError.QueryTimeout(scopeId, timeoutMs)
            
            error.shouldBeInstanceOf<ExistsScopeError.QueryTimeout>()
            error.scopeId shouldBe scopeId
            error.timeoutMs shouldBe timeoutMs
        }
        
        test("should create ConnectionFailure error with message and cause") {
            val message = "Database connection lost"
            val cause = RuntimeException("Connection reset")
            val error = ExistsScopeError.ConnectionFailure(message, cause)
            
            error.shouldBeInstanceOf<ExistsScopeError.ConnectionFailure>()
            error.message shouldBe message
            error.cause shouldBe cause
        }
        
        test("should create IndexCorruption error with scope ID and message") {
            val scopeId = ScopeId.generate()
            val message = "Index is corrupted and needs rebuilding"
            val error = ExistsScopeError.IndexCorruption(scopeId, message)
            
            error.shouldBeInstanceOf<ExistsScopeError.IndexCorruption>()
            error.scopeId shouldBe scopeId
            error.message shouldBe message
        }
        
        test("should create PersistenceFailure error with message and cause") {
            val message = "Storage system unavailable"
            val cause = RuntimeException("Disk full")
            val error = ExistsScopeError.PersistenceFailure(message, cause)
            
            error.shouldBeInstanceOf<ExistsScopeError.PersistenceFailure>()
            error.message shouldBe message
            error.cause shouldBe cause
        }
        
        test("should create UnknownError error with message and cause") {
            val message = "Unexpected error during existence check"
            val cause = RuntimeException("Unknown failure")
            val error = ExistsScopeError.UnknownError(message, cause)
            
            error.shouldBeInstanceOf<ExistsScopeError.UnknownError>()
            error.message shouldBe message
            error.cause shouldBe cause
        }
    }
    
    context("ExistsScopeError sealed hierarchy") {
        
        test("all error types should be instances of ExistsScopeError") {
            val scopeId = ScopeId.generate()
            val cause = RuntimeException("Test")
            
            val errors = listOf(
                ExistsScopeError.QueryTimeout(scopeId, 5000L),
                ExistsScopeError.ConnectionFailure("test", cause),
                ExistsScopeError.IndexCorruption(scopeId, "test"),
                ExistsScopeError.PersistenceFailure("test", cause),
                ExistsScopeError.UnknownError("test", cause)
            )
            
            errors.forEach { error ->
                error.shouldBeInstanceOf<ExistsScopeError>()
            }
        }
    }
})