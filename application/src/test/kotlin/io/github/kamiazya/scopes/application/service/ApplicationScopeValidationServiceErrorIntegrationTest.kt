package io.github.kamiazya.scopes.application.service

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError
import io.github.kamiazya.scopes.domain.error.ScopeValidationServiceError
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.valueobject.ScopeTitle
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
            
            it("should return Right for valid short title (domain MIN_LENGTH=1)") {
                val result = service.validateTitleFormat("a".repeat(ScopeTitle.MIN_LENGTH + 1)) // Valid since domain MIN_LENGTH is ${ScopeTitle.MIN_LENGTH}
                
                result.isRight() shouldBe true
            }
            
            it("should return ScopeValidationServiceError.TitleValidationError.EmptyTitle for whitespace-only title") {
                val result = service.validateTitleFormat("   ") // Becomes empty after trim
                
                result.isLeft() shouldBe true
                result.leftOrNull().shouldBeInstanceOf<ScopeValidationServiceError.TitleValidationError.EmptyTitle>()
            }
            
            it("should return ScopeValidationServiceError.TitleValidationError.TooLong for long title") {
                val longTitle = "a".repeat(ApplicationScopeValidationService.MAX_TITLE_LENGTH + 1) // Exceeds service MAX_TITLE_LENGTH of ${ApplicationScopeValidationService.MAX_TITLE_LENGTH}
                val result = service.validateTitleFormat(longTitle)
                
                result.isLeft() shouldBe true
                val error = result.leftOrNull().shouldBeInstanceOf<ScopeValidationServiceError.TitleValidationError.TooLong>()
                error.actualLength shouldBe ApplicationScopeValidationService.MAX_TITLE_LENGTH + 1
                error.maxLength shouldBe ApplicationScopeValidationService.MAX_TITLE_LENGTH
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
                coEvery { mockRepository.findHierarchyDepth(parentId) } returns (ApplicationScopeValidationService.MAX_HIERARCHY_DEPTH + 1).right() // Over the limit
                
                val result = service.validateHierarchyConstraints(parentId)
                
                result.isLeft() shouldBe true
                val error = result.leftOrNull().shouldBeInstanceOf<BusinessRuleServiceError.ScopeBusinessRuleError.MaxDepthExceeded>()
                error.maxDepth shouldBe ApplicationScopeValidationService.MAX_HIERARCHY_DEPTH
                error.attemptedDepth shouldBe ApplicationScopeValidationService.MAX_HIERARCHY_DEPTH + 2
            }
            
            it("should return BusinessRuleServiceError.ScopeBusinessRuleError.MaxChildrenExceeded for children limit") {
                val parentId = ScopeId.generate()
                coEvery { mockRepository.findHierarchyDepth(parentId) } returns 5.right()
                coEvery { mockRepository.countByParentId(parentId) } returns ApplicationScopeValidationService.MAX_CHILDREN_PER_PARENT.right() // At the limit
                
                val result = service.validateHierarchyConstraints(parentId)
                
                result.isLeft() shouldBe true
                val error = result.leftOrNull().shouldBeInstanceOf<BusinessRuleServiceError.ScopeBusinessRuleError.MaxChildrenExceeded>()
                error.maxChildren shouldBe ApplicationScopeValidationService.MAX_CHILDREN_PER_PARENT
                error.currentChildren shouldBe ApplicationScopeValidationService.MAX_CHILDREN_PER_PARENT
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