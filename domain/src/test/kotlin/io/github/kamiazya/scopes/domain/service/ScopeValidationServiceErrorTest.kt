package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.error.ScopeValidationServiceError
import io.github.kamiazya.scopes.domain.error.TitleValidationError
import io.github.kamiazya.scopes.domain.error.DescriptionValidationError
import io.github.kamiazya.scopes.domain.error.HierarchyValidationError
import io.github.kamiazya.scopes.domain.error.UniquenessValidationError
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
                val scopeId = ScopeId.generate()
                val error = HierarchyValidationError.DepthLimitExceeded(
                    maxDepth = 10,
                    actualDepth = 12,
                    scopeId = scopeId
                )
                error.maxDepth shouldBe 10
                error.actualDepth shouldBe 12
                error.scopeId shouldBe scopeId
            }
            it("should provide context for invalid parent reference errors") {
                val scopeId = ScopeId.generate()
                val parentId = ScopeId.generate()
                val error = HierarchyValidationError.InvalidParentReference(
                    scopeId = scopeId,
                    parentId = parentId,
                    reason = "Parent scope does not exist"
                )
                error.scopeId shouldBe scopeId
                error.parentId shouldBe parentId
                error.reason shouldBe "Parent scope does not exist"
            }
            it("should provide context for circular hierarchy errors") {
                val scopeId = ScopeId.generate()
                val parentId = ScopeId.generate()
                val cyclePath = listOf(scopeId, parentId, scopeId)
                val error = HierarchyValidationError.CircularHierarchy(
                    scopeId = scopeId,
                    parentId = parentId,
                    cyclePath = cyclePath
                )
                error.scopeId shouldBe scopeId
                error.parentId shouldBe parentId
                error.cyclePath shouldBe cyclePath
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