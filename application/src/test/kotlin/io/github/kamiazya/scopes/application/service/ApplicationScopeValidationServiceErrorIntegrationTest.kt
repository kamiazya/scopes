package io.github.kamiazya.scopes.application.service

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError
import io.github.kamiazya.scopes.domain.error.ScopeValidationServiceError
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
            it("should return ScopeValidationServiceError.TitleValidationError.EmptyTitle for empty title") {
                val result = service.validateTitleFormat("")
                
                result.isLeft() shouldBe true
                result.leftOrNull().shouldBeInstanceOf<ScopeValidationServiceError.TitleValidationError.EmptyTitle>()
            }
            
            it("should return ScopeValidationServiceError.TitleValidationError.TooShort for short title") {
                val result = service.validateTitleFormat("ab") // Assuming min length is 3
                
                result.isLeft() shouldBe true
                val error = result.leftOrNull().shouldBeInstanceOf<ScopeValidationServiceError.TitleValidationError.TooShort>()
                error.actualLength shouldBe 2
            }
            
            it("should return ScopeValidationServiceError.TitleValidationError.TooLong for long title") {
                val longTitle = "a".repeat(200) // Assuming max length is less than 200
                val result = service.validateTitleFormat(longTitle)
                
                result.isLeft() shouldBe true
                val error = result.leftOrNull().shouldBeInstanceOf<ScopeValidationServiceError.TitleValidationError.TooLong>()
                error.actualLength shouldBe 200
            }
            
            it("should return ScopeValidationServiceError.TitleValidationError.InvalidCharacters for newline characters") {
                val result = service.validateTitleFormat("Title with\nnewline")
                
                result.isLeft() shouldBe true
                val error = result.leftOrNull().shouldBeInstanceOf<ScopeValidationServiceError.TitleValidationError.InvalidCharacters>()
                error.invalidChars.contains('\n') shouldBe true
            }
            
            it("should return Right for valid title") {
                val result = service.validateTitleFormat("Valid Title")
                
                result.isRight() shouldBe true
            }
        }
        
        describe("hierarchy validation with service-specific errors") {
            it("should return BusinessRuleServiceError.ScopeBusinessRuleError.MaxDepthExceeded for depth limit") {
                val parentId = ScopeId.generate()
                coEvery { mockRepository.findHierarchyDepth(parentId) } returns 11.right() // Over the limit of 10
                
                val result = service.validateHierarchyConstraints(parentId)
                
                result.isLeft() shouldBe true
                val error = result.leftOrNull().shouldBeInstanceOf<BusinessRuleServiceError.ScopeBusinessRuleError.MaxDepthExceeded>()
                error.maxDepth shouldBe 10
                error.attemptedDepth shouldBe 12
            }
            
            it("should return BusinessRuleServiceError.ScopeBusinessRuleError.MaxChildrenExceeded for children limit") {
                val parentId = ScopeId.generate()
                coEvery { mockRepository.findHierarchyDepth(parentId) } returns 5.right()
                coEvery { mockRepository.countByParentId(parentId) } returns 100.right() // At the limit of 100
                
                val result = service.validateHierarchyConstraints(parentId)
                
                result.isLeft() shouldBe true
                val error = result.leftOrNull().shouldBeInstanceOf<BusinessRuleServiceError.ScopeBusinessRuleError.MaxChildrenExceeded>()
                error.maxChildren shouldBe 100
                error.currentChildren shouldBe 100
            }
            
            it("should return Right for valid hierarchy") {
                val parentId = ScopeId.generate()
                coEvery { mockRepository.findHierarchyDepth(parentId) } returns 5.right()
                coEvery { mockRepository.countByParentId(parentId) } returns 50.right()
                
                val result = service.validateHierarchyConstraints(parentId)
                
                result.isRight() shouldBe true
            }
        }
        
        describe("uniqueness validation with service-specific errors") {
            it("should return ScopeValidationServiceError.UniquenessValidationError.DuplicateTitle for duplicate") {
                val parentId = ScopeId.generate()
                coEvery { mockRepository.existsByParentIdAndTitle(parentId, "duplicate title") } returns true.right()
                
                val result = service.validateTitleUniquenessTyped("Duplicate Title", parentId)
                
                result.isLeft() shouldBe true
                val error = result.leftOrNull().shouldBeInstanceOf<ScopeValidationServiceError.UniquenessValidationError.DuplicateTitle>()
                error.title shouldBe "Duplicate Title"
                error.parentId shouldBe parentId
                error.normalizedTitle shouldBe "duplicate title"
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