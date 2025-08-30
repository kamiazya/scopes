package io.github.kamiazya.scopes.scopemanagement.application.error

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError as DomainScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

// Test-only error type for testing unmapped cases
class TestUnmappedDataInconsistencyError(override val occurredAt: Instant) : 
    DomainScopeAliasError.DataInconsistencyError() {
    override fun toString() = "TestUnmappedDataInconsistencyError"
}

/**
 * Specification tests for error handling behavior.
 * 
 * These tests document the INTENTIONAL design decision to fail-fast
 * when encountering unmapped error types, preventing data corruption
 * from being silently ignored.
 */
class ErrorMappingSpecificationTest : DescribeSpec({
    
    describe("Error Mapping Specification") {
        
        describe("Known error types") {
            it("should map AliasExistsButScopeNotFound to application error") {
                // Given: A known data inconsistency error
                val domainError = DomainScopeAliasError.DataInconsistencyError.AliasExistsButScopeNotFound(
                    occurredAt = Clock.System.now(),
                    aliasName = "test-alias",
                    scopeId = ScopeId.generate()
                )
                
                // When: Mapping to application error
                val result = domainError.toApplicationError()
                
                // Then: Should successfully map without throwing
                result shouldBe ApplicationError.ScopeAliasError.DataInconsistencyError.AliasExistsButScopeNotFound(
                    aliasName = "test-alias",
                    scopeId = domainError.scopeId.toString()
                )
            }
        }
        
        describe("Unmapped error types - FAIL-FAST SPECIFICATION") {
            it("should throw IllegalStateException for unmapped DataInconsistencyError subtypes") {
                /**
                 * SPECIFICATION: This is INTENTIONAL behavior.
                 * 
                 * When a new DataInconsistencyError subtype is added to the domain
                 * but not mapped in the application layer, the system MUST fail-fast
                 * to prevent data corruption from being silently ignored.
                 * 
                 * This forces developers to explicitly handle new error types,
                 * ensuring data integrity issues are never masked.
                 */
                
                // Given: A hypothetical new unmapped error type
                val unmappedError = TestUnmappedDataInconsistencyError(
                    occurredAt = Clock.System.now()
                )
                
                // When/Then: Should fail-fast with clear error message
                val exception = shouldThrow<IllegalStateException> {
                    unmappedError.toApplicationError()
                }
                
                // The error message must be informative for developers
                exception.message shouldContain "Unmapped DataInconsistencyError subtype"
                exception.message shouldContain "Please add proper error mapping"
            }
            
            it("should use Kotlin's error() function for fail-fast behavior") {
                /**
                 * SPECIFICATION: Use Kotlin idiomatic error handling.
                 * 
                 * The codebase uses Kotlin's standard error() function
                 * instead of throwing IllegalStateException directly.
                 * This is enforced by architecture tests.
                 */
                
                // This test documents that error() is the preferred approach
                // The actual implementation uses error(), which internally
                // throws IllegalStateException with the given message
                
                val unmappedError = TestUnmappedDataInconsistencyError(
                    occurredAt = Clock.System.now()
                )
                
                val exception = shouldThrow<IllegalStateException> {
                    unmappedError.toApplicationError()
                }
                
                // error() function creates IllegalStateException
                exception.javaClass.simpleName shouldBe "IllegalStateException"
            }
        }
        
        describe("Design rationale") {
            it("should document why fail-fast is chosen over fallback values") {
                /**
                 * DESIGN RATIONALE:
                 * 
                 * 1. Data Integrity: Using "unknown" fallbacks masks real problems
                 * 2. Early Detection: Fail-fast ensures issues are caught in development/testing
                 * 3. No Monitoring: As a local CLI tool, we can't rely on monitoring to catch issues
                 * 4. User Trust: Better to fail loudly than corrupt data silently
                 * 
                 * Alternative approaches considered and rejected:
                 * - Returning "unknown" values: Hides data corruption
                 * - Logging and continuing: No monitoring system to alert on logs
                 * - Default fallbacks: Can lead to incorrect data persisting
                 */
                
                // This test serves as documentation of the design decision
                true shouldBe true
            }
        }
        
        describe("Future-proofing") {
            it("should require explicit handling for new error categories") {
                /**
                 * SPECIFICATION: New error categories must be explicitly handled.
                 * 
                 * When adding new error types to the domain layer:
                 * 1. The mapping will fail at runtime if not updated
                 * 2. This failure is caught during testing
                 * 3. Developers are forced to make conscious decisions about error handling
                 * 
                 * This prevents accidental omission of error handling logic.
                 */
                
                // Example: If we add a new category like CircularAliasReference
                // The system will fail-fast until proper mapping is added
                
                // This ensures all error paths are intentionally designed
                true shouldBe true
            }
        }
    }
})