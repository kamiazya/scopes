package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.error.ScopeValidationServiceError
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Test class for ScopeValidationServiceError hierarchy.
 * 
 * Tests verify that service-specific error types provide appropriate
 * context and can be used for type-safe error handling.
 */
class ScopeValidationServiceErrorTest : DescribeSpec({

    describe("ScopeValidationServiceError hierarchy") {
        
        describe("TitleValidationError") {
            it("should provide context for empty title errors") {
                val error = ScopeValidationServiceError.TitleValidationError.EmptyTitle
                
                error.shouldBeInstanceOf<ScopeValidationServiceError.TitleValidationError>()
                error.shouldBeInstanceOf<ScopeValidationServiceError>()
            }
            
            it("should provide context for title too short errors") {
                val error = ScopeValidationServiceError.TitleValidationError.TooShort(
                    minLength = 3,
                    actualLength = 1
                )
                
                error.minLength shouldBe 3
                error.actualLength shouldBe 1
            }
            
            it("should provide context for title too long errors") {
                val error = ScopeValidationServiceError.TitleValidationError.TooLong(
                    maxLength = 100,
                    actualLength = 150
                )
                
                error.maxLength shouldBe 100
                error.actualLength shouldBe 150
            }
            
            it("should provide context for invalid character errors") {
                val error = ScopeValidationServiceError.TitleValidationError.InvalidCharacters(
                    invalidChars = setOf('\n', '\r')
                )
                
                error.invalidChars shouldBe setOf('\n', '\r')
            }
        }
        
        describe("DescriptionValidationError") {
            it("should provide context for description too long errors") {
                val error = ScopeValidationServiceError.DescriptionValidationError.TooLong(
                    maxLength = 1000,
                    actualLength = 1500
                )
                
                error.maxLength shouldBe 1000
                error.actualLength shouldBe 1500
            }
        }
        
        describe("HierarchyValidationError") {
            it("should provide context for depth exceeded errors") {
                val parentId = ScopeId.generate()
                val error = ScopeValidationServiceError.HierarchyValidationError.DepthExceeded(
                    maxDepth = 10,
                    currentDepth = 12,
                    parentId = parentId
                )
                
                error.maxDepth shouldBe 10
                error.currentDepth shouldBe 12
                error.parentId shouldBe parentId
            }
            
            it("should provide context for children limit exceeded errors") {
                val parentId = ScopeId.generate()
                val error = ScopeValidationServiceError.HierarchyValidationError.ChildrenLimitExceeded(
                    maxChildren = 100,
                    currentChildren = 105,
                    parentId = parentId
                )
                
                error.maxChildren shouldBe 100
                error.currentChildren shouldBe 105
                error.parentId shouldBe parentId
            }
            
            it("should provide context for circular reference errors") {
                val scopeId = ScopeId.generate()
                val parentId = ScopeId.generate()
                val error = ScopeValidationServiceError.HierarchyValidationError.CircularReference(
                    scopeId = scopeId,
                    parentId = parentId
                )
                
                error.scopeId shouldBe scopeId
                error.parentId shouldBe parentId
            }
        }
        
        describe("UniquenessValidationError") {
            it("should provide context for duplicate title errors") {
                val parentId = ScopeId.generate()
                val error = ScopeValidationServiceError.UniquenessValidationError.DuplicateTitle(
                    title = "Existing Title",
                    parentId = parentId,
                    normalizedTitle = "existing title"
                )
                
                error.title shouldBe "Existing Title"
                error.parentId shouldBe parentId
                error.normalizedTitle shouldBe "existing title"
            }
        }
    }
})