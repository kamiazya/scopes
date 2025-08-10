package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Test cases for FindScopeError sealed class following TDD methodology.
 * These tests define the expected behavior for hierarchy traversal failures.
 */
class FindScopeErrorTest : FunSpec({
    
    context("FindScopeError sealed class") {
        
        test("should create TraversalTimeout error with scope ID and timeout duration") {
            val scopeId = ScopeId.generate()
            val timeoutMillis = 15000L
            val error = FindScopeError.TraversalTimeout(scopeId, timeoutMillis)
            
            error.shouldBeInstanceOf<FindScopeError.TraversalTimeout>()
            error.scopeId shouldBe scopeId
            error.timeoutMillis shouldBe timeoutMillis
        }
        
        test("should create CircularReference error with scope ID and path") {
            val scopeId = ScopeId.generate()
            val cyclePath = listOf(ScopeId.generate(), scopeId, ScopeId.generate())
            val error = FindScopeError.CircularReference(scopeId, cyclePath)
            
            error.shouldBeInstanceOf<FindScopeError.CircularReference>()
            error.scopeId shouldBe scopeId
            error.cyclePath shouldBe cyclePath
        }
        
        test("should create OrphanedScope error with scope ID and message") {
            val scopeId = ScopeId.generate()
            val message = "Parent scope not found during traversal"
            val error = FindScopeError.OrphanedScope(scopeId, message)
            
            error.shouldBeInstanceOf<FindScopeError.OrphanedScope>()
            error.scopeId shouldBe scopeId
            error.message shouldBe message
        }
        
        test("should create ConnectionFailure error with scope ID, message and cause") {
            val scopeId = ScopeId.generate()
            val message = "Database connection lost during hierarchy traversal"
            val cause = RuntimeException("Connection reset")
            val error = FindScopeError.ConnectionFailure(scopeId, message, cause)
            
            error.shouldBeInstanceOf<FindScopeError.ConnectionFailure>()
            error.scopeId shouldBe scopeId
            error.message shouldBe message
            error.cause shouldBe cause
        }
        
        test("should create PersistenceFailure error with scope ID, message and cause") {
            val scopeId = ScopeId.generate()
            val message = "Storage system unavailable"
            val cause = RuntimeException("Disk full")
            val error = FindScopeError.PersistenceFailure(scopeId, message, cause)
            
            error.shouldBeInstanceOf<FindScopeError.PersistenceFailure>()
            error.scopeId shouldBe scopeId
            error.message shouldBe message
            error.cause shouldBe cause
        }
        
        test("should create UnknownError error with scope ID, message and cause") {
            val scopeId = ScopeId.generate()
            val message = "Unexpected error during hierarchy traversal"
            val cause = RuntimeException("Unknown failure")
            val error = FindScopeError.UnknownError(scopeId, message, cause)
            
            error.shouldBeInstanceOf<FindScopeError.UnknownError>()
            error.scopeId shouldBe scopeId
            error.message shouldBe message
            error.cause shouldBe cause
        }
    }
    
    context("FindScopeError sealed hierarchy") {
        
        test("all error types should be instances of FindScopeError") {
            val scopeId = ScopeId.generate()
            val cyclePath = listOf(scopeId)
            val cause = RuntimeException("Test")
            
            val errors = listOf(
                FindScopeError.TraversalTimeout(scopeId, 15000L),
                FindScopeError.CircularReference(scopeId, cyclePath),
                FindScopeError.OrphanedScope(scopeId, "test"),
                FindScopeError.ConnectionFailure(scopeId, "test", cause),
                FindScopeError.PersistenceFailure(scopeId, "test", cause),
                FindScopeError.UnknownError(scopeId, "test", cause)
            )
            
            errors.forEach { error ->
                error.shouldBeInstanceOf<FindScopeError>()
            }
        }
    }
})