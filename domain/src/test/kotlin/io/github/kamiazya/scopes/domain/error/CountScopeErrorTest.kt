package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Test cases for CountScopeError sealed class following TDD methodology.
 * These tests define the expected behavior for count operation failures.
 */
class CountScopeErrorTest : FunSpec({
    
    context("CountScopeError sealed class") {
        
        test("should create AggregationTimeout error with timeout duration") {
            val timeoutMillis = 10000L
            val error = CountScopeError.AggregationTimeout(timeoutMillis)
            
            error.shouldBeInstanceOf<CountScopeError.AggregationTimeout>()
            error.timeoutMillis shouldBe timeoutMillis
        }
        
        test("should create ConnectionFailure error with parent ID, message and cause") {
            val parentId = ScopeId.generate()
            val message = "Database connection lost during count"
            val cause = RuntimeException("Connection reset")
            val error = CountScopeError.ConnectionFailure(parentId, message, cause)
            
            error.shouldBeInstanceOf<CountScopeError.ConnectionFailure>()
            error.parentId shouldBe parentId
            error.message shouldBe message
            error.cause shouldBe cause
        }
        
        test("should create InvalidParentId error with parent ID and message") {
            val parentId = ScopeId.generate()
            val message = "Parent scope does not exist"
            val error = CountScopeError.InvalidParentId(parentId, message)
            
            error.shouldBeInstanceOf<CountScopeError.InvalidParentId>()
            error.parentId shouldBe parentId
            error.message shouldBe message
        }
        
        test("should create PersistenceFailure error with parent ID, message and cause") {
            val parentId = ScopeId.generate()
            val message = "Storage system unavailable"
            val cause = RuntimeException("Disk full")
            val error = CountScopeError.PersistenceFailure(parentId, message, cause)
            
            error.shouldBeInstanceOf<CountScopeError.PersistenceFailure>()
            error.parentId shouldBe parentId
            error.message shouldBe message
            error.cause shouldBe cause
        }
        
        test("should create UnknownError error with parent ID, message and cause") {
            val parentId = ScopeId.generate()
            val message = "Unexpected error during count operation"
            val cause = RuntimeException("Unknown failure")
            val error = CountScopeError.UnknownError(parentId, message, cause)
            
            error.shouldBeInstanceOf<CountScopeError.UnknownError>()
            error.parentId shouldBe parentId
            error.message shouldBe message
            error.cause shouldBe cause
        }
    }
    
    context("CountScopeError sealed hierarchy") {
        
        test("all error types should be instances of CountScopeError") {
            val parentId = ScopeId.generate()
            val cause = RuntimeException("Test")
            
            val errors = listOf(
                CountScopeError.AggregationTimeout(10000L),
                CountScopeError.ConnectionFailure(parentId, "test", cause),
                CountScopeError.InvalidParentId(parentId, "test"),
                CountScopeError.PersistenceFailure(parentId, "test", cause),
                CountScopeError.UnknownError(parentId, "test", cause)
            )
            
            errors.forEach { error ->
                error.shouldBeInstanceOf<CountScopeError>()
            }
        }
    }
})