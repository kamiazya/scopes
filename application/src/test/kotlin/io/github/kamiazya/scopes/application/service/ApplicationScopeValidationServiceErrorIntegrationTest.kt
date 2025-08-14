package io.github.kamiazya.scopes.application.service

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.domain.error.ScopeBusinessRuleError
import io.github.kamiazya.scopes.domain.error.ScopeValidationServiceError
import io.github.kamiazya.scopes.domain.error.TitleValidationError
import io.github.kamiazya.scopes.domain.error.UniquenessValidationError
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk

/**
 * Integration tests for ApplicationScopeValidationService using new service-specific error types.
 * 
 * These tests verify that the service properly translates domain errors to service-specific
 * error hierarchies.
 */
class ApplicationScopeValidationServiceErrorIntegrationTest : DescribeSpec({

    describe("ApplicationScopeValidationService error integration") {
        val mockRepository = mockk<ScopeRepository>()
        val service = ApplicationScopeValidationService(mockRepository)
        
        describe("title validation with service-specific errors") {
            it("should return TitleValidationError.EmptyTitle for empty title") {
                val result = service.validateTitleFormat("")
                
                result.isLeft() shouldBe true
                result.leftOrNull().shouldBeInstanceOf<TitleValidationError.EmptyTitle>()
            }
            
            it("should return Right for valid title") {
                val result = service.validateTitleFormat("Valid Title")
                
                result.isRight() shouldBe true
            }
            
            it("should return TitleValidationError.EmptyTitle for whitespace-only title") {
                val result = service.validateTitleFormat("   ")
                
                result.isLeft() shouldBe true
                result.leftOrNull().shouldBeInstanceOf<TitleValidationError.EmptyTitle>()
            }
            
            it("should return TitleValidationError.TitleTooLong for excessively long title") {
                val longTitle = "a".repeat(1000) // Very long title
                val result = service.validateTitleFormat(longTitle)
                
                result.isLeft() shouldBe true
                result.leftOrNull().shouldBeInstanceOf<TitleValidationError.TitleTooLong>()
            }
            
            it("should return TitleValidationError.InvalidCharacters for newline characters") {
                val result = service.validateTitleFormat("Title with\nnewline")
                
                result.isLeft() shouldBe true
                result.leftOrNull().shouldBeInstanceOf<TitleValidationError.InvalidCharacters>()
            }
        }
        
        describe("hierarchy validation with service-specific errors") {
            it("should return ScopeBusinessRuleError.MaxDepthExceeded for depth limit") {
                val parentId = ScopeId.generate()
                coEvery { mockRepository.findHierarchyDepth(parentId) } returns 15.right() // Over a reasonable limit
                
                val result = service.validateHierarchyConstraints(parentId)
                
                result.isLeft() shouldBe true
                result.leftOrNull().shouldBeInstanceOf<ScopeBusinessRuleError.MaxDepthExceeded>()
            }
            
            it("should return ScopeBusinessRuleError.MaxChildrenExceeded for children limit") {
                val parentId = ScopeId.generate()
                coEvery { mockRepository.findHierarchyDepth(parentId) } returns 5.right()
                coEvery { mockRepository.countByParentId(parentId) } returns 1000.right() // Many children
                
                val result = service.validateHierarchyConstraints(parentId)
                
                result.isLeft() shouldBe true
                result.leftOrNull().shouldBeInstanceOf<ScopeBusinessRuleError.MaxChildrenExceeded>()
            }
            
            it("should return Right for valid hierarchy") {
                val parentId = ScopeId.generate()
                coEvery { mockRepository.findHierarchyDepth(parentId) } returns 3.right()
                coEvery { mockRepository.countByParentId(parentId) } returns 5.right()
                
                val result = service.validateHierarchyConstraints(parentId)
                
                result.isRight() shouldBe true
            }
        }
        
        describe("uniqueness validation with service-specific errors") {
            it("should return UniquenessValidationError.DuplicateTitle for duplicate") {
                val parentId = ScopeId.generate()
                coEvery { mockRepository.existsByParentIdAndTitle(parentId, "duplicate title") } returns true.right()
                
                val result = service.validateTitleUniquenessTyped("Duplicate Title", parentId)
                
                result.isLeft() shouldBe true
                result.leftOrNull().shouldBeInstanceOf<UniquenessValidationError.DuplicateTitle>()
            }
            
            it("should return Right for unique title") {
                val parentId = ScopeId.generate()
                coEvery { mockRepository.existsByParentIdAndTitle(parentId, "unique title") } returns false.right()
                
                val result = service.validateTitleUniquenessTyped("Unique Title", parentId)
                
                result.isRight() shouldBe true
            }
        }
    }
})