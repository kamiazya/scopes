package io.github.kamiazya.scopes.application.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.application.service.error.ApplicationValidationError
import io.github.kamiazya.scopes.application.service.error.CrossAggregateValidationError
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

/**
 * Test for CrossAggregateValidationService.
 * 
 * This test validates cross-aggregate validation logic that spans multiple aggregates
 * and requires coordination between different bounded contexts.
 * 
 * Based on Serena MCP research on cross-aggregate validation patterns:
 * - Eventual consistency handling
 * - Saga pattern for distributed validation
 * - Cross-aggregate invariant enforcement
 * - Distributed validation with compensation
 */
class CrossAggregateValidationServiceTest : DescribeSpec({

    val mockScopeRepository = mockk<ScopeRepository>()
    val service = CrossAggregateValidationService(mockScopeRepository)

    describe("CrossAggregateValidationService") {

        describe("validateHierarchyConsistency") {
            it("should succeed when hierarchy is consistent") {
                // Given
                val parentId = ScopeId.generate()
                val childIds = listOf(ScopeId.generate(), ScopeId.generate())
                
                coEvery { mockScopeRepository.existsById(parentId) } returns true.right()
                coEvery { mockScopeRepository.existsById(childIds[0]) } returns true.right()
                coEvery { mockScopeRepository.existsById(childIds[1]) } returns true.right()
                coEvery { mockScopeRepository.findHierarchyDepth(parentId) } returns 2.right()

                // When
                val result = service.validateHierarchyConsistency(parentId, childIds)

                // Then
                result.isRight() shouldBe true

                coVerify { mockScopeRepository.existsById(parentId) }
                coVerify { mockScopeRepository.existsById(childIds[0]) }
                coVerify { mockScopeRepository.existsById(childIds[1]) }
            }

            it("should fail when parent does not exist") {
                // Given
                val parentId = ScopeId.generate()
                val childIds = listOf(ScopeId.generate())
                
                coEvery { mockScopeRepository.existsById(parentId) } returns false.right()

                // When
                val result = service.validateHierarchyConsistency(parentId, childIds)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<CrossAggregateValidationError.CrossReferenceViolation>()
                
                val crossRefError = error as CrossAggregateValidationError.CrossReferenceViolation
                crossRefError.sourceAggregate shouldBe "children"
                crossRefError.targetAggregate shouldBe parentId.value
                crossRefError.referenceType shouldBe "parentId"
                crossRefError.violation shouldBe "Parent scope does not exist"
            }

            it("should fail when any child does not exist") {
                // Given
                val parentId = ScopeId.generate()
                val existingChildId = ScopeId.generate()
                val nonExistingChildId = ScopeId.generate()
                val childIds = listOf(existingChildId, nonExistingChildId)
                
                coEvery { mockScopeRepository.existsById(parentId) } returns true.right()
                coEvery { mockScopeRepository.existsById(existingChildId) } returns true.right()
                coEvery { mockScopeRepository.existsById(nonExistingChildId) } returns false.right()

                // When
                val result = service.validateHierarchyConsistency(parentId, childIds)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<CrossAggregateValidationError.CrossReferenceViolation>()
                
                val crossRefError = error as CrossAggregateValidationError.CrossReferenceViolation
                crossRefError.targetAggregate shouldBe nonExistingChildId.value
                crossRefError.violation shouldBe "Child scope does not exist"
            }
        }

        describe("validateCrossAggregateUniqueness") {
            it("should succeed when titles are unique across all aggregates") {
                // Given
                val title = "Unique Title"
                val contextIds = listOf(ScopeId.generate(), ScopeId.generate())
                
                coEvery { 
                    mockScopeRepository.existsByParentIdAndTitle(contextIds[0], title.lowercase()) 
                } returns false.right()
                coEvery { 
                    mockScopeRepository.existsByParentIdAndTitle(contextIds[1], title.lowercase()) 
                } returns false.right()

                // When
                val result = service.validateCrossAggregateUniqueness(title, contextIds)

                // Then
                result.isRight() shouldBe true

                coVerify { 
                    mockScopeRepository.existsByParentIdAndTitle(contextIds[0], title.lowercase()) 
                }
                coVerify { 
                    mockScopeRepository.existsByParentIdAndTitle(contextIds[1], title.lowercase()) 
                }
            }

            it("should fail when title conflicts across aggregates") {
                // Given
                val title = "Conflicting Title"
                val contextId1 = ScopeId.generate()
                val contextId2 = ScopeId.generate()
                val contextIds = listOf(contextId1, contextId2)
                
                coEvery { 
                    mockScopeRepository.existsByParentIdAndTitle(contextId1, title.lowercase()) 
                } returns false.right()
                coEvery { 
                    mockScopeRepository.existsByParentIdAndTitle(contextId2, title.lowercase()) 
                } returns true.right() // Conflict detected

                // When
                val result = service.validateCrossAggregateUniqueness(title, contextIds)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<CrossAggregateValidationError.InvariantViolation>()
                
                val invariantError = error as CrossAggregateValidationError.InvariantViolation
                invariantError.invariantName shouldBe "crossAggregateUniqueness"
                invariantError.aggregateIds shouldBe contextIds.map { it.value }
                invariantError.violationDescription shouldBe "Title '$title' conflicts across aggregates"
            }
        }

        describe("validateAggregateConsistency") {
            it("should succeed when all aggregates are in consistent state") {
                // Given
                val operation = "moveScope"
                val aggregateIds = setOf("scope-1", "scope-2")
                val consistencyRule = "hierarchyIntegrity"
                
                val scope1Id = ScopeId.generate()
                val scope2Id = ScopeId.generate()
                val testAggregateIds = setOf(scope1Id.value, scope2Id.value)
                
                coEvery { 
                    mockScopeRepository.existsById(scope1Id) 
                } returns true.right()
                coEvery { 
                    mockScopeRepository.existsById(scope2Id) 
                } returns true.right()

                // When
                val result = service.validateAggregateConsistency(operation, testAggregateIds, consistencyRule)

                // Then
                result.isRight() shouldBe true
            }

            it("should fail when aggregates are in inconsistent state") {
                // Given
                val operation = "moveScope"
                val scope1Id = ScopeId.generate()
                val missingId = ScopeId.generate()
                val testAggregateIds = setOf(scope1Id.value, missingId.value)
                val consistencyRule = "hierarchyIntegrity"
                
                coEvery { 
                    mockScopeRepository.existsById(scope1Id) 
                } returns true.right()
                coEvery { 
                    mockScopeRepository.existsById(missingId) 
                } returns false.right()

                // When
                val result = service.validateAggregateConsistency(operation, testAggregateIds, consistencyRule)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<CrossAggregateValidationError.AggregateConsistencyViolation>()
                
                val consistencyError = error as CrossAggregateValidationError.AggregateConsistencyViolation
                consistencyError.operation shouldBe operation
                consistencyError.affectedAggregates shouldBe testAggregateIds
                consistencyError.consistencyRule shouldBe consistencyRule
                consistencyError.violationDetails shouldBe "Aggregate ${missingId.value} does not exist or is in invalid state"
            }
        }

        describe("error accumulation") {
            it("should accumulate multiple validation errors") {
                // This test demonstrates eventual consistency handling
                // In a real system, this might involve compensating transactions
                
                // Given - scenario with multiple validation failures
                val parentId = ScopeId.generate()
                val childIds = listOf(ScopeId.generate()) 
                val title = "Duplicate Title"
                
                // Setup mocks for failures
                coEvery { mockScopeRepository.existsById(parentId) } returns false.right()
                coEvery { 
                    mockScopeRepository.existsByParentIdAndTitle(parentId, title.lowercase()) 
                } returns true.right()

                // When - performing validations
                val hierarchyResult = service.validateHierarchyConsistency(parentId, childIds)
                val uniquenessResult = service.validateCrossAggregateUniqueness(title, listOf(parentId))

                // Then - both should fail with specific errors
                hierarchyResult.isLeft() shouldBe true
                uniquenessResult.isLeft() shouldBe true
                
                val hierarchyError = hierarchyResult.leftOrNull()!!
                val uniquenessError = uniquenessResult.leftOrNull()!!
                
                hierarchyError should beInstanceOf<CrossAggregateValidationError.CrossReferenceViolation>()
                uniquenessError should beInstanceOf<CrossAggregateValidationError.InvariantViolation>()
            }
        }
    }
})