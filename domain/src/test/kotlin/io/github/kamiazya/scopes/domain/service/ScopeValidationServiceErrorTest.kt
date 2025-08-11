package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.error.ScopeValidationServiceError
import io.github.kamiazya.scopes.domain.error.TitleValidationError
import io.github.kamiazya.scopes.domain.error.DescriptionValidationError
import io.github.kamiazya.scopes.domain.error.HierarchyValidationError
import io.github.kamiazya.scopes.domain.error.UniquenessValidationError
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
                val error = TitleValidationError.EmptyTitle
                
                error.shouldBeInstanceOf<TitleValidationError>()
                error.shouldBeInstanceOf<ScopeValidationServiceError>()
            }
            
            it("should provide context for title too short errors") {
                val error = TitleValidationError.TitleTooShort(
                    minLength = 3,
                    actualLength = 1,
                    title = "a"
                )
                
                error.minLength shouldBe 3
                error.actualLength shouldBe 1
                error.title shouldBe "a"
            }
            
            it("should provide context for title too long errors") {
                val error = TitleValidationError.TitleTooLong(
                    maxLength = 100,
                    actualLength = 150,
                    title = "very long title..."
                )
                
                error.maxLength shouldBe 100
                error.actualLength shouldBe 150
                error.title shouldBe "very long title..."
            }
            
            it("should provide context for invalid character errors") {
                val error = TitleValidationError.InvalidCharacters(
                    title = "title\nwith\nnewlines",
                    invalidCharacters = setOf('\n'),
                    position = 5
                )
                
                error.title shouldBe "title\nwith\nnewlines"
                error.invalidCharacters shouldBe setOf('\n')
                error.position shouldBe 5
            }
        }
        
        describe("DescriptionValidationError") {
            it("should provide context for description too long errors") {
                val error = DescriptionValidationError.DescriptionTooLong(
                    maxLength = 1000,
                    actualLength = 1500
                )
                
                error.maxLength shouldBe 1000
                error.actualLength shouldBe 1500
            }
        }
        
        describe("HierarchyValidationError") {
            it("should provide context for depth limit exceeded errors") {
                val error = HierarchyValidationError.DepthLimitExceeded(
                    maxDepth = 10,
                    actualDepth = 12,
                    scopeId = "test-scope-id"
                )
                
                error.maxDepth shouldBe 10
                error.actualDepth shouldBe 12
                error.scopeId shouldBe "test-scope-id"
            }
            
            it("should provide context for invalid parent reference errors") {
                val error = HierarchyValidationError.InvalidParentReference(
                    scopeId = "child-scope-id",
                    parentId = "invalid-parent-id",
                    reason = "Parent scope does not exist"
                )
                
                error.scopeId shouldBe "child-scope-id"
                error.parentId shouldBe "invalid-parent-id"
                error.reason shouldBe "Parent scope does not exist"
            }
            
            it("should provide context for circular hierarchy errors") {
                val error = HierarchyValidationError.CircularHierarchy(
                    scopeId = "scope-a",
                    parentId = "scope-b",
                    cyclePath = listOf("scope-a", "scope-b", "scope-a")
                )
                
                error.scopeId shouldBe "scope-a"
                error.parentId shouldBe "scope-b"
                error.cyclePath shouldBe listOf("scope-a", "scope-b", "scope-a")
            }
        }
        
        describe("UniquenessValidationError") {
            it("should provide context for duplicate title errors") {
                val error = UniquenessValidationError.DuplicateTitle(
                    title = "Existing Title",
                    normalizedTitle = "existing title",
                    parentId = "parent-id",
                    existingScopeId = "existing-scope-id"
                )
                
                error.title shouldBe "Existing Title"
                error.normalizedTitle shouldBe "existing title"
                error.parentId shouldBe "parent-id"
                error.existingScopeId shouldBe "existing-scope-id"
            }
        }
    }
})