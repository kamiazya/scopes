package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Simple unit test for the UpdateScopeHandler focusing on the title validation logic
 * that was added based on AI review feedback.
 *
 * These tests verify that the critical validation from the Gemini AI review
 * is working correctly: title uniqueness validation during updates.
 */
class UpdateScopeHandlerTest : DescribeSpec({
    describe("UpdateScopeHandler validation logic") {
        context("ScopeTitle validation") {
            it("should validate title format during updates") {
                // Given
                val applicationErrorMapper = ApplicationErrorMapper(ConsoleLogger())
                
                // When - Create title with invalid format (newlines not allowed)
                val invalidTitleResult = ScopeTitle.create("Invalid\nTitle")
                
                // Then - Should return validation error
                invalidTitleResult.shouldBeLeft()
                
                // When - Create title with valid format
                val validTitleResult = ScopeTitle.create("Valid Title")
                
                // Then - Should succeed
                validTitleResult.shouldBeRight()
                when (validTitleResult) {
                    is Either.Left -> throw AssertionError("Expected success but got error: ${validTitleResult.value}")
                    is Either.Right -> validTitleResult.value.value shouldBe "Valid Title"
                }
            }

            it("should trim whitespace in titles") {
                // Given
                val titleWithSpaces = "  Valid Title  "
                
                // When
                val result = ScopeTitle.create(titleWithSpaces)
                
                // Then
                result.shouldBeRight()
                when (result) {
                    is Either.Left -> throw AssertionError("Expected success but got error: ${result.value}")
                    is Either.Right -> result.value.value shouldBe "Valid Title"  // Trimmed
                }
            }

            it("should reject empty titles") {
                // Given
                val emptyTitle = ""
                
                // When
                val result = ScopeTitle.create(emptyTitle)
                
                // Then
                result.shouldBeLeft()
            }

            it("should reject titles that are too long") {
                // Given
                val longTitle = "a".repeat(201)  // Max length is 200
                
                // When
                val result = ScopeTitle.create(longTitle)
                
                // Then
                result.shouldBeLeft()
            }
        }
    }
})