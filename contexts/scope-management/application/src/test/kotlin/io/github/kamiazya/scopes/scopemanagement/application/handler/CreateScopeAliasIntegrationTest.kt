package io.github.kamiazya.scopes.scopemanagement.application.handler

import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Clock

/**
 * Integration tests for CreateScope alias functionality.
 * Tests focus on alias validation and business logic without complex mocking.
 */
class CreateScopeAliasIntegrationTest :
    DescribeSpec({

        describe("CreateScope Alias Integration - Validation Tests") {

            describe("AliasName validation") {
                it("should handle empty alias") {
                    // Given
                    val emptyAlias = ""

                    // When
                    val result = AliasName.create(emptyAlias)

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error.shouldBeInstanceOf<ScopeInputError.AliasError.Empty>()
                            val emptyError = error as ScopeInputError.AliasError.Empty
                            emptyError.attemptedValue shouldBe emptyAlias
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )
                }

                it("should handle too short alias") {
                    // Given
                    val shortAlias = "a"

                    // When
                    val result = AliasName.create(shortAlias)

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error.shouldBeInstanceOf<ScopeInputError.AliasError.TooShort>()
                            val shortError = error as ScopeInputError.AliasError.TooShort
                            shortError.attemptedValue shouldBe shortAlias
                            shortError.minimumLength shouldBe 2
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )
                }

                it("should handle too long alias") {
                    // Given
                    val longAlias = "a".repeat(65) // Exceeds 64 character limit

                    // When
                    val result = AliasName.create(longAlias)

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error.shouldBeInstanceOf<ScopeInputError.AliasError.TooLong>()
                            val longError = error as ScopeInputError.AliasError.TooLong
                            longError.attemptedValue shouldBe longAlias
                            longError.maximumLength shouldBe 64
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )
                }

                it("should handle invalid format - special characters at start") {
                    // Given
                    val invalidAlias = "-invalid-start"

                    // When
                    val result = AliasName.create(invalidAlias)

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error.shouldBeInstanceOf<ScopeInputError.AliasError.InvalidFormat>()
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )
                }

                it("should handle invalid format - special characters at end") {
                    // Given
                    val invalidAlias = "invalid-end_"

                    // When
                    val result = AliasName.create(invalidAlias)

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error.shouldBeInstanceOf<ScopeInputError.AliasError.InvalidFormat>()
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )
                }

                it("should handle consecutive special characters") {
                    // Given
                    val invalidAlias = "invalid--consecutive"

                    // When
                    val result = AliasName.create(invalidAlias)

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error.shouldBeInstanceOf<ScopeInputError.AliasError.InvalidFormat>()
                            val formatError = error as ScopeInputError.AliasError.InvalidFormat
                            formatError.expectedPattern shouldBe "no consecutive hyphens or underscores"
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )
                }

                it("should handle invalid characters") {
                    // Given
                    val invalidAlias = "invalid@alias!"

                    // When
                    val result = AliasName.create(invalidAlias)

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error.shouldBeInstanceOf<ScopeInputError.AliasError.InvalidFormat>()
                            val formatError = error as ScopeInputError.AliasError.InvalidFormat
                            formatError.attemptedValue shouldBe invalidAlias
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )
                }

                it("should accept valid aliases") {
                    // Given
                    val validAliases = listOf(
                        "ab",
                        "valid-alias",
                        "valid_alias",
                        "valid-alias-123",
                        "alias123",
                        "123alias",
                        "a".repeat(64), // Maximum length
                    )

                    validAliases.forEach { validAlias ->
                        // When
                        val result = AliasName.create(validAlias)

                        // Then
                        result.isRight() shouldBe true
                        result.fold(
                            { throw AssertionError("Expected Right but got Left for '$validAlias': $it") },
                            { aliasName ->
                                aliasName.value shouldBe validAlias
                            },
                        )
                    }
                }

                it("should handle whitespace trimming") {
                    // Given
                    val aliasWithWhitespace = "  valid-alias  "
                    val expectedTrimmed = "valid-alias"

                    // When
                    val result = AliasName.create(aliasWithWhitespace)

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { aliasName ->
                            aliasName.value shouldBe expectedTrimmed
                        },
                    )
                }
            }

            describe("Edge cases") {
                it("should handle blank string after trimming") {
                    // Given
                    val blankAlias = "   "

                    // When
                    val result = AliasName.create(blankAlias)

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error.shouldBeInstanceOf<ScopeInputError.AliasError.Empty>()
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )
                }

                it("should handle mixed case aliases") {
                    // Given
                    val mixedCaseAlias = "Valid-Alias-123"

                    // When
                    val result = AliasName.create(mixedCaseAlias)

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { aliasName ->
                            aliasName.value shouldBe mixedCaseAlias
                        },
                    )
                }

                it("should handle numeric aliases") {
                    // Given
                    val numericAlias = "12"

                    // When
                    val result = AliasName.create(numericAlias)

                    // Then
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Expected Right but got Left: $it") },
                        { aliasName ->
                            aliasName.value shouldBe numericAlias
                        },
                    )
                }
            }

            describe("Business rule validation") {
                it("should ensure all error instances have proper timestamp") {
                    // Given
                    val before = Clock.System.now()

                    // When
                    val result = AliasName.create("")

                    val after = Clock.System.now()

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error.shouldBeInstanceOf<ScopeInputError.AliasError.Empty>()
                            val emptyError = error as ScopeInputError.AliasError.Empty
                            (emptyError.occurredAt.epochSeconds >= before.epochSeconds) shouldBe true
                            (emptyError.occurredAt.epochSeconds <= after.epochSeconds) shouldBe true
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )
                }

                it("should preserve original attempted value in error messages") {
                    // Given
                    val originalValue = "  invalid@value!  "

                    // When
                    val result = AliasName.create(originalValue)

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            error.shouldBeInstanceOf<ScopeInputError.AliasError.InvalidFormat>()
                            val formatError = error as ScopeInputError.AliasError.InvalidFormat
                            // Should preserve trimmed value in error
                            formatError.attemptedValue shouldBe "invalid@value!"
                        },
                        { throw AssertionError("Expected Left but got Right: $it") },
                    )
                }
            }
        }
    })
