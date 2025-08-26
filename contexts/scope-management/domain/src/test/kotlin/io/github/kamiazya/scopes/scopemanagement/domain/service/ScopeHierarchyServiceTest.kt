package io.github.kamiazya.scopes.scopemanagement.domain.service

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Test suite for ScopeHierarchyService pure functions.
 * These tests verify business logic without any I/O operations or mocks.
 */
@DisplayName("ScopeHierarchyService")
class ScopeHierarchyServiceTest {

    private lateinit var service: ScopeHierarchyService

    @BeforeEach
    fun setup() {
        service = ScopeHierarchyService()
    }

    @Nested
    @DisplayName("calculateDepth")
    inner class CalculateDepthTest {

        @Test
        fun `should return 0 for empty hierarchy path`() {
            val result = service.calculateDepth(emptyList())
            assertEquals(0, result)
        }

        @Test
        fun `should return 1 for single scope`() {
            val path = listOf(ScopeId("scope1"))
            val result = service.calculateDepth(path)
            assertEquals(1, result)
        }

        @Test
        fun `should return correct depth for multi-level hierarchy`() {
            val path = listOf(
                ScopeId("child"),
                ScopeId("parent"),
                ScopeId("grandparent"),
                ScopeId("root"),
            )
            val result = service.calculateDepth(path)
            assertEquals(4, result)
        }
    }

    @Nested
    @DisplayName("detectCircularReference")
    inner class DetectCircularReferenceTest {

        @Test
        fun `should return success for valid hierarchy without cycles`() {
            val path = listOf(
                ScopeId("scope1"),
                ScopeId("scope2"),
                ScopeId("scope3"),
            )
            val result = service.detectCircularReference(path)
            assertTrue(result.isRight())
        }

        @Test
        fun `should detect circular reference when scope appears twice`() {
            val path = listOf(
                ScopeId("scope1"),
                ScopeId("scope2"),
                ScopeId("scope1"), // Circular reference
            )
            val result = service.detectCircularReference(path)

            assertTrue(result.isLeft())
            val error = (result as Either.Left).value
            assertTrue(error is ScopeHierarchyError.CircularPath)
            assertEquals(ScopeId("scope1"), (error as ScopeHierarchyError.CircularPath).scopeId)
        }

        @Test
        fun `should detect complex circular reference`() {
            val path = listOf(
                ScopeId("a"),
                ScopeId("b"),
                ScopeId("c"),
                ScopeId("d"),
                ScopeId("b"), // Circular reference back to b
            )
            val result = service.detectCircularReference(path)

            assertTrue(result.isLeft())
            val error = (result as Either.Left).value
            assertTrue(error is ScopeHierarchyError.CircularPath)
        }

        @Test
        fun `should return success for empty path`() {
            val result = service.detectCircularReference(emptyList())
            assertTrue(result.isRight())
        }
    }

    @Nested
    @DisplayName("validateParentChildRelationship")
    inner class ValidateParentChildRelationshipTest {

        @Test
        fun `should detect self-parenting`() {
            val scopeId = ScopeId("scope1")
            val result = service.validateParentChildRelationship(
                parentId = scopeId,
                childId = scopeId,
                parentAncestorPath = emptyList(),
            )

            assertTrue(result.isLeft())
            val error = (result as Either.Left).value
            assertTrue(error is ScopeHierarchyError.SelfParenting)
            assertEquals(scopeId, (error as ScopeHierarchyError.SelfParenting).scopeId)
        }

        @Test
        fun `should detect circular reference when child is in parent's ancestors`() {
            val childId = ScopeId("child")
            val parentId = ScopeId("parent")
            val parentAncestorPath = listOf(
                ScopeId("grandparent"),
                childId, // Child appears in parent's ancestors
                ScopeId("root"),
            )

            val result = service.validateParentChildRelationship(
                parentId = parentId,
                childId = childId,
                parentAncestorPath = parentAncestorPath,
            )

            assertTrue(result.isLeft())
            val error = (result as Either.Left).value
            assertTrue(error is ScopeHierarchyError.CircularReference)
            val circularError = error as ScopeHierarchyError.CircularReference
            assertEquals(childId, circularError.childScopeId)
            assertEquals(parentId, circularError.parentScopeId)
        }

        @Test
        fun `should allow valid parent-child relationship`() {
            val parentId = ScopeId("parent")
            val childId = ScopeId("child")
            val parentAncestorPath = listOf(
                ScopeId("grandparent"),
                ScopeId("root"),
            )

            val result = service.validateParentChildRelationship(
                parentId = parentId,
                childId = childId,
                parentAncestorPath = parentAncestorPath,
            )

            assertTrue(result.isRight())
        }

        @Test
        fun `should allow relationship when parent has no ancestors`() {
            val result = service.validateParentChildRelationship(
                parentId = ScopeId.generate(),
                childId = ScopeId("child"),
                parentAncestorPath = emptyList(),
            )

            assertTrue(result.isRight())
        }
    }

    @Nested
    @DisplayName("validateChildrenLimit")
    inner class ValidateChildrenLimitTest {

        @Test
        fun `should allow when limit is not reached`() {
            val result = service.validateChildrenLimit(
                parentId = ScopeId.generate(),
                currentChildCount = 5,
                maxChildren = 10,
            )
            assertTrue(result.isRight())
        }

        @Test
        fun `should reject when limit is reached`() {
            val parentId = ScopeId("parent")
            val result = service.validateChildrenLimit(
                parentId = parentId,
                currentChildCount = 10,
                maxChildren = 10,
            )

            assertTrue(result.isLeft())
            val error = (result as Either.Left).value
            assertTrue(error is ScopeHierarchyError.MaxChildrenExceeded)
            val limitError = error as ScopeHierarchyError.MaxChildrenExceeded
            assertEquals(parentId, limitError.parentScopeId)
            assertEquals(10, limitError.currentChildrenCount)
            assertEquals(10, limitError.maximumChildren)
        }

        @Test
        fun `should allow unlimited children when maxChildren is null`() {
            val result = service.validateChildrenLimit(
                parentId = ScopeId.generate(),
                currentChildCount = 1000,
                maxChildren = null,
            )
            assertTrue(result.isRight())
        }

        @Test
        fun `should allow zero children when limit is greater than zero`() {
            val result = service.validateChildrenLimit(
                parentId = ScopeId.generate(),
                currentChildCount = 0,
                maxChildren = 5,
            )
            assertTrue(result.isRight())
        }
    }

    @Nested
    @DisplayName("validateHierarchyDepth")
    inner class ValidateHierarchyDepthTest {

        @Test
        fun `should allow when depth is within limit`() {
            val result = service.validateHierarchyDepth(
                scopeId = ScopeId.generate(),
                currentDepth = 3,
                maxDepth = 5,
            )
            assertTrue(result.isRight())
        }

        @Test
        fun `should allow when depth equals limit`() {
            val result = service.validateHierarchyDepth(
                scopeId = ScopeId.generate(),
                currentDepth = 4,
                maxDepth = 5,
            )
            assertTrue(result.isRight())
        }

        @Test
        fun `should reject when depth exceeds limit`() {
            val scopeId = ScopeId("scope")
            val result = service.validateHierarchyDepth(
                scopeId = scopeId,
                currentDepth = 5,
                maxDepth = 5,
            )

            assertTrue(result.isLeft())
            val error = (result as Either.Left).value
            assertTrue(error is ScopeHierarchyError.MaxDepthExceeded)
            val depthError = error as ScopeHierarchyError.MaxDepthExceeded
            assertEquals(scopeId, depthError.scopeId)
            assertEquals(6, depthError.attemptedDepth) // currentDepth + 1
            assertEquals(5, depthError.maximumDepth)
        }

        @Test
        fun `should allow unlimited depth when maxDepth is null`() {
            val result = service.validateHierarchyDepth(
                scopeId = ScopeId.generate(),
                currentDepth = 1000,
                maxDepth = null,
            )
            assertTrue(result.isRight())
        }

        @Test
        fun `should allow root level when depth is 0`() {
            val result = service.validateHierarchyDepth(
                scopeId = ScopeId.generate(),
                currentDepth = 0,
                maxDepth = 5,
            )
            assertTrue(result.isRight())
        }
    }
}
