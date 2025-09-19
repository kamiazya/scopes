package io.github.kamiazya.scopes.scopemanagement.domain.error

import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for ScopeHierarchyError and its nested error types.
 */
class ScopeHierarchyErrorTest :
    StringSpec({

        "should create CircularDependency error" {
            val scopeId = ScopeId.generate()
            val ancestorId = ScopeId.generate()

            val error = ScopeHierarchyError.CircularDependency(
                scopeId = scopeId,
                ancestorId = ancestorId,
            )

            error.shouldBeInstanceOf<ScopeHierarchyError>()
            error.shouldBeInstanceOf<ScopesError>()
            error.scopeId shouldBe scopeId
            error.ancestorId shouldBe ancestorId
        }

        "should detect circular reference in hierarchy" {
            // Scenario: A -> B -> C -> A (circular)
            val scopeA = ScopeId.generate()
            val scopeC = ScopeId.generate()

            val error = ScopeHierarchyError.CircularDependency(
                scopeId = scopeC, // Trying to set C's parent to A
                ancestorId = scopeA, // But A is already a descendant of C
            )

            error.scopeId shouldBe scopeC
            error.ancestorId shouldBe scopeA
        }

        "should create MaxDepthExceeded error" {
            val scopeId = ScopeId.generate()

            val error = ScopeHierarchyError.MaxDepthExceeded(
                scopeId = scopeId,
                currentDepth = 11,
                maxDepth = 10,
            )

            error.shouldBeInstanceOf<ScopeHierarchyError>()
            error.shouldBeInstanceOf<ScopesError>()
            error.scopeId shouldBe scopeId
            error.currentDepth shouldBe 11
            error.maxDepth shouldBe 10
        }

        "should handle various depth limit scenarios" {
            val testCases = listOf(
                Triple(5, 5, "At limit"),
                Triple(6, 5, "One over limit"),
                Triple(100, 10, "Way over limit"),
            )

            testCases.forEach { (current, max, description) ->
                val error = ScopeHierarchyError.MaxDepthExceeded(
                    scopeId = ScopeId.generate(),
                    currentDepth = current,
                    maxDepth = max,
                )
                error.currentDepth shouldBe current
                error.maxDepth shouldBe max
                // Description is just for test clarity
            }
        }

        "should create MaxChildrenExceeded error" {
            val parentId = ScopeId.generate()

            val error = ScopeHierarchyError.MaxChildrenExceeded(
                parentId = parentId,
                currentCount = 101,
                maxChildren = 100,
            )

            error.shouldBeInstanceOf<ScopeHierarchyError>()
            error.shouldBeInstanceOf<ScopesError>()
            error.parentId shouldBe parentId
            error.currentCount shouldBe 101
            error.maxChildren shouldBe 100
        }

        "should handle various children limit scenarios" {
            val testCases = listOf(
                Triple(50, 50, "At limit"),
                Triple(51, 50, "One over limit"),
                Triple(1000, 100, "Way over limit"),
            )

            testCases.forEach { (current, max, description) ->
                val error = ScopeHierarchyError.MaxChildrenExceeded(
                    parentId = ScopeId.generate(),
                    currentCount = current,
                    maxChildren = max,
                )
                error.currentCount shouldBe current
                error.maxChildren shouldBe max
                // Description is just for test clarity
            }
        }

        "should create HierarchyUnavailable error with all parameters" {
            val scopeId = ScopeId.generate()

            val error = ScopeHierarchyError.HierarchyUnavailable(
                scopeId = scopeId,
                operation = HierarchyOperation.TRAVERSE_ANCESTORS,
                reason = AvailabilityReason.TEMPORARILY_UNAVAILABLE,
            )

            error.shouldBeInstanceOf<ScopeHierarchyError>()
            error.shouldBeInstanceOf<ScopesError>()
            error.scopeId shouldBe scopeId
            error.operation shouldBe HierarchyOperation.TRAVERSE_ANCESTORS
            error.reason shouldBe AvailabilityReason.TEMPORARILY_UNAVAILABLE
        }

        "should create HierarchyUnavailable error without scopeId" {
            val error = ScopeHierarchyError.HierarchyUnavailable(
                scopeId = null,
                operation = HierarchyOperation.COUNT_CHILDREN,
                reason = AvailabilityReason.CORRUPTED_HIERARCHY,
            )

            error.scopeId shouldBe null
            error.operation shouldBe HierarchyOperation.COUNT_CHILDREN
            error.reason shouldBe AvailabilityReason.CORRUPTED_HIERARCHY
        }

        "should support all HierarchyOperation values" {
            val operations = listOf(
                HierarchyOperation.TRAVERSE_ANCESTORS,
                HierarchyOperation.COUNT_CHILDREN,
                HierarchyOperation.FIND_DESCENDANTS,
                HierarchyOperation.VERIFY_EXISTENCE,
                HierarchyOperation.RETRIEVE_SCOPE,
            )

            operations.forEach { operation ->
                val error = ScopeHierarchyError.HierarchyUnavailable(
                    operation = operation,
                    reason = AvailabilityReason.TEMPORARILY_UNAVAILABLE,
                )
                error.operation shouldBe operation
            }
        }

        "should support all AvailabilityReason values" {
            val reasons = listOf(
                AvailabilityReason.TEMPORARILY_UNAVAILABLE,
                AvailabilityReason.CORRUPTED_HIERARCHY,
                AvailabilityReason.CONCURRENT_MODIFICATION,
            )

            reasons.forEach { reason ->
                val error = ScopeHierarchyError.HierarchyUnavailable(
                    operation = HierarchyOperation.VERIFY_EXISTENCE,
                    reason = reason,
                )
                error.reason shouldBe reason
            }
        }

        // Test specific business scenarios
        "should handle concurrent modification scenario" {
            val scopeId = ScopeId.generate()

            val error = ScopeHierarchyError.HierarchyUnavailable(
                scopeId = scopeId,
                operation = HierarchyOperation.TRAVERSE_ANCESTORS,
                reason = AvailabilityReason.CONCURRENT_MODIFICATION,
            )

            // This represents a case where hierarchy traversal failed because
            // another operation modified the hierarchy concurrently
            error.operation shouldBe HierarchyOperation.TRAVERSE_ANCESTORS
            error.reason shouldBe AvailabilityReason.CONCURRENT_MODIFICATION
        }

        "should handle corrupted hierarchy scenario" {
            val error = ScopeHierarchyError.HierarchyUnavailable(
                scopeId = null, // Might not know specific scope if corruption detected
                operation = HierarchyOperation.FIND_DESCENDANTS,
                reason = AvailabilityReason.CORRUPTED_HIERARCHY,
            )

            // This represents detecting data integrity issues in the hierarchy
            error.operation shouldBe HierarchyOperation.FIND_DESCENDANTS
            error.reason shouldBe AvailabilityReason.CORRUPTED_HIERARCHY
        }

        // Test error equality and data class behavior
        "should properly implement equals and hashCode for CircularDependency" {
            val scopeId = ScopeId.generate()
            val ancestorId = ScopeId.generate()

            val error1 = ScopeHierarchyError.CircularDependency(scopeId, ancestorId)
            val error2 = ScopeHierarchyError.CircularDependency(scopeId, ancestorId)
            val error3 = ScopeHierarchyError.CircularDependency(ancestorId, scopeId) // Swapped

            error1 shouldBe error2
            error1.hashCode() shouldBe error2.hashCode()
            error1 shouldNotBe error3
        }

        "should maintain proper inheritance hierarchy for all error types" {
            val errors: List<ScopeHierarchyError> = listOf(
                ScopeHierarchyError.CircularDependency(ScopeId.generate(), ScopeId.generate()),
                ScopeHierarchyError.MaxDepthExceeded(ScopeId.generate(), 10, 5),
                ScopeHierarchyError.MaxChildrenExceeded(ScopeId.generate(), 100, 50),
                ScopeHierarchyError.HierarchyUnavailable(
                    operation = HierarchyOperation.VERIFY_EXISTENCE,
                    reason = AvailabilityReason.TEMPORARILY_UNAVAILABLE,
                ),
            )

            errors.forEach { error ->
                error.shouldBeInstanceOf<ScopeHierarchyError>()
                error.shouldBeInstanceOf<ScopesError>()
            }
        }
    })
