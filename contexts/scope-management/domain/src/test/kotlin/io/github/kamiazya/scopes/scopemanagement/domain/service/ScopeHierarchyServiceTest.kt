package io.github.kamiazya.scopes.scopemanagement.domain.service

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Test suite for ScopeHierarchyService pure functions.
 * These tests verify business logic without any I/O operations or mocks.
 */
class ScopeHierarchyServiceTest :
    DescribeSpec({
        val service = ScopeHierarchyService()

        describe("ScopeHierarchyService") {

            describe("calculateDepth") {
                it("should return 0 for empty hierarchy path") {
                    val result = service.calculateDepth(emptyList())
                    result shouldBe 0
                }

                it("should return 1 for single scope") {
                    val path = listOf(ScopeId.generate())
                    val result = service.calculateDepth(path)
                    result shouldBe 1
                }

                it("should return correct depth for multi-level hierarchy") {
                    val path = listOf(
                        ScopeId.generate(),
                        ScopeId.generate(),
                        ScopeId.generate(),
                        ScopeId.generate(),
                    )
                    val result = service.calculateDepth(path)
                    result shouldBe 4
                }
            }

            describe("detectCircularReference") {
                it("should return success for valid hierarchy without cycles") {
                    val path = listOf(
                        ScopeId.generate(),
                        ScopeId.generate(),
                        ScopeId.generate(),
                    )
                    val result = service.detectCircularReference(path)
                    result.isRight() shouldBe true
                }

                it("should detect circular reference when scope appears twice") {
                    val scope1 = ScopeId.generate()
                    val scope2 = ScopeId.generate()
                    val path = listOf(
                        scope1,
                        scope2,
                        scope1, // Circular reference
                    )
                    val result = service.detectCircularReference(path)

                    result.isLeft() shouldBe true
                    val error = (result as Either.Left).value
                    error.shouldBeInstanceOf<ScopeHierarchyError.CircularPath>()
                    (error as ScopeHierarchyError.CircularPath).scopeId shouldBe scope1
                }

                it("should detect complex circular reference") {
                    val a = ScopeId.generate()
                    val b = ScopeId.generate()
                    val c = ScopeId.generate()
                    val d = ScopeId.generate()
                    val path = listOf(
                        a,
                        b,
                        c,
                        d,
                        b, // Circular reference back to b
                    )
                    val result = service.detectCircularReference(path)

                    result.isLeft() shouldBe true
                    val error = (result as Either.Left).value
                    error.shouldBeInstanceOf<ScopeHierarchyError.CircularPath>()
                }

                it("should return success for empty path") {
                    val result = service.detectCircularReference(emptyList())
                    result.isRight() shouldBe true
                }
            }

            describe("validateParentChildRelationship") {
                it("should detect self-parenting") {
                    val scopeId = ScopeId.generate()
                    val result = service.validateParentChildRelationship(
                        parentId = scopeId,
                        childId = scopeId,
                        parentAncestorPath = emptyList(),
                    )

                    result.isLeft() shouldBe true
                    val error = (result as Either.Left).value
                    error.shouldBeInstanceOf<ScopeHierarchyError.SelfParenting>()
                    (error as ScopeHierarchyError.SelfParenting).scopeId shouldBe scopeId
                }

                it("should detect circular reference when child is in parent's ancestors") {
                    val childId = ScopeId.generate()
                    val parentId = ScopeId.generate()
                    val grandparent = ScopeId.generate()
                    val root = ScopeId.generate()
                    val parentAncestorPath = listOf(
                        grandparent,
                        childId, // Child appears in parent's ancestors
                        root,
                    )

                    val result = service.validateParentChildRelationship(
                        parentId = parentId,
                        childId = childId,
                        parentAncestorPath = parentAncestorPath,
                    )

                    result.isLeft() shouldBe true
                    val error = (result as Either.Left).value
                    error.shouldBeInstanceOf<ScopeHierarchyError.CircularReference>()
                    val circularError = error as ScopeHierarchyError.CircularReference
                    circularError.scopeId shouldBe childId
                    circularError.parentId shouldBe parentId
                }

                it("should allow valid parent-child relationship") {
                    val parentId = ScopeId.generate()
                    val childId = ScopeId.generate()
                    val parentAncestorPath = listOf(
                        ScopeId.generate(),
                        ScopeId.generate(),
                    )

                    val result = service.validateParentChildRelationship(
                        parentId = parentId,
                        childId = childId,
                        parentAncestorPath = parentAncestorPath,
                    )

                    result.isRight() shouldBe true
                }

                it("should allow relationship when parent has no ancestors") {
                    val result = service.validateParentChildRelationship(
                        parentId = ScopeId.generate(),
                        childId = ScopeId.generate(),
                        parentAncestorPath = emptyList(),
                    )

                    result.isRight() shouldBe true
                }
            }

            describe("validateChildrenLimit") {
                it("should allow when limit is not reached") {
                    val result = service.validateChildrenLimit(
                        parentId = ScopeId.generate(),
                        currentChildCount = 5,
                        maxChildren = 10,
                    )
                    result.isRight() shouldBe true
                }

                it("should reject when limit is reached") {
                    val parentId = ScopeId.generate()
                    val result = service.validateChildrenLimit(
                        parentId = parentId,
                        currentChildCount = 10,
                        maxChildren = 10,
                    )

                    result.isLeft() shouldBe true
                    val error = (result as Either.Left).value
                    error.shouldBeInstanceOf<ScopeHierarchyError.MaxChildrenExceeded>()
                    val limitError = error as ScopeHierarchyError.MaxChildrenExceeded
                    limitError.parentScopeId shouldBe parentId
                    limitError.currentChildrenCount shouldBe 10
                    limitError.maximumChildren shouldBe 10
                }

                it("should allow unlimited children when maxChildren is null") {
                    val result = service.validateChildrenLimit(
                        parentId = ScopeId.generate(),
                        currentChildCount = 1000,
                        maxChildren = null,
                    )
                    result.isRight() shouldBe true
                }

                it("should allow zero children when limit is greater than zero") {
                    val result = service.validateChildrenLimit(
                        parentId = ScopeId.generate(),
                        currentChildCount = 0,
                        maxChildren = 5,
                    )
                    result.isRight() shouldBe true
                }
            }

            describe("validateHierarchyDepth") {
                it("should allow when depth is within limit") {
                    val result = service.validateHierarchyDepth(
                        scopeId = ScopeId.generate(),
                        currentDepth = 3,
                        maxDepth = 5,
                    )
                    result.isRight() shouldBe true
                }

                it("should allow when depth equals limit") {
                    val result = service.validateHierarchyDepth(
                        scopeId = ScopeId.generate(),
                        currentDepth = 4,
                        maxDepth = 5,
                    )
                    result.isRight() shouldBe true
                }

                it("should reject when depth exceeds limit") {
                    val scopeId = ScopeId.generate()
                    val result = service.validateHierarchyDepth(
                        scopeId = scopeId,
                        currentDepth = 5,
                        maxDepth = 5,
                    )

                    result.isLeft() shouldBe true
                    val error = (result as Either.Left).value
                    error.shouldBeInstanceOf<ScopeHierarchyError.MaxDepthExceeded>()
                    val depthError = error as ScopeHierarchyError.MaxDepthExceeded
                    depthError.scopeId shouldBe scopeId
                    depthError.attemptedDepth shouldBe 6 // currentDepth + 1
                    depthError.maximumDepth shouldBe 5
                }

                it("should allow unlimited depth when maxDepth is null") {
                    val result = service.validateHierarchyDepth(
                        scopeId = ScopeId.generate(),
                        currentDepth = 1000,
                        maxDepth = null,
                    )
                    result.isRight() shouldBe true
                }

                it("should allow root level when depth is 0") {
                    val result = service.validateHierarchyDepth(
                        scopeId = ScopeId.generate(),
                        currentDepth = 0,
                        maxDepth = 5,
                    )
                    result.isRight() shouldBe true
                }
            }
        }
    })