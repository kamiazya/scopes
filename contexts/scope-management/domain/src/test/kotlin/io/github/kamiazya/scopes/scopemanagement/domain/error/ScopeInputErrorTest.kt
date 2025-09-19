package io.github.kamiazya.scopes.scopemanagement.domain.error

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for ScopeInputError and its nested error types.
 */
class ScopeInputErrorTest :
    StringSpec({

        // IdError tests
        "should create EmptyId error" {
            val error = ScopeInputError.IdError.EmptyId

            error.shouldBeInstanceOf<ScopeInputError>()
            error.shouldBeInstanceOf<ScopeInputError.IdError>()
            error.shouldBeInstanceOf<ScopeInputError.IdError.EmptyId>()
        }

        "should create InvalidIdFormat error with ULID format type" {
            val error = ScopeInputError.IdError.InvalidIdFormat(
                id = "invalid-id-123",
                expectedFormat = ScopeInputError.IdError.InvalidIdFormat.IdFormatType.ULID,
            )

            error.shouldBeInstanceOf<ScopeInputError>()
            error.shouldBeInstanceOf<ScopeInputError.IdError>()
            error.id shouldBe "invalid-id-123"
            error.expectedFormat shouldBe ScopeInputError.IdError.InvalidIdFormat.IdFormatType.ULID
        }

        "should support all IdFormatType values" {
            val formatTypes = listOf(
                ScopeInputError.IdError.InvalidIdFormat.IdFormatType.ULID,
                ScopeInputError.IdError.InvalidIdFormat.IdFormatType.UUID,
                ScopeInputError.IdError.InvalidIdFormat.IdFormatType.NUMERIC_ID,
                ScopeInputError.IdError.InvalidIdFormat.IdFormatType.CUSTOM_FORMAT,
            )

            formatTypes.forEach { formatType ->
                val error = ScopeInputError.IdError.InvalidIdFormat(
                    id = "test-id",
                    expectedFormat = formatType,
                )
                error.expectedFormat shouldBe formatType
            }
        }

        // TitleError tests
        "should create EmptyTitle error" {
            val error = ScopeInputError.TitleError.EmptyTitle

            error.shouldBeInstanceOf<ScopeInputError>()
            error.shouldBeInstanceOf<ScopeInputError.TitleError>()
            error.shouldBeInstanceOf<ScopeInputError.TitleError.EmptyTitle>()
        }

        "should create TitleTooShort error" {
            val error = ScopeInputError.TitleError.TitleTooShort(minLength = 3)

            error.shouldBeInstanceOf<ScopeInputError>()
            error.shouldBeInstanceOf<ScopeInputError.TitleError>()
            error.minLength shouldBe 3
        }

        "should create TitleTooLong error" {
            val error = ScopeInputError.TitleError.TitleTooLong(maxLength = 200)

            error.shouldBeInstanceOf<ScopeInputError>()
            error.shouldBeInstanceOf<ScopeInputError.TitleError>()
            error.maxLength shouldBe 200
        }

        "should create InvalidTitleFormat error" {
            val error = ScopeInputError.TitleError.InvalidTitleFormat(
                title = "Title with\nnewline",
            )

            error.shouldBeInstanceOf<ScopeInputError>()
            error.shouldBeInstanceOf<ScopeInputError.TitleError>()
            error.title shouldBe "Title with\nnewline"
        }

        // DescriptionError tests
        "should create DescriptionTooLong error" {
            val error = ScopeInputError.DescriptionError.DescriptionTooLong(maxLength = 5000)

            error.shouldBeInstanceOf<ScopeInputError>()
            error.shouldBeInstanceOf<ScopeInputError.DescriptionError>()
            error.maxLength shouldBe 5000
        }

        // AliasError tests
        "should create EmptyAlias error" {
            val error = ScopeInputError.AliasError.EmptyAlias

            error.shouldBeInstanceOf<ScopeInputError>()
            error.shouldBeInstanceOf<ScopeInputError.AliasError>()
            error.shouldBeInstanceOf<ScopeInputError.AliasError.EmptyAlias>()
        }

        "should create AliasTooShort error" {
            val error = ScopeInputError.AliasError.AliasTooShort(minLength = 2)

            error.shouldBeInstanceOf<ScopeInputError>()
            error.shouldBeInstanceOf<ScopeInputError.AliasError>()
            error.minLength shouldBe 2
        }

        "should create AliasTooLong error" {
            val error = ScopeInputError.AliasError.AliasTooLong(maxLength = 64)

            error.shouldBeInstanceOf<ScopeInputError>()
            error.shouldBeInstanceOf<ScopeInputError.AliasError>()
            error.maxLength shouldBe 64
        }

        "should create InvalidAliasFormat error with LOWERCASE_WITH_HYPHENS pattern" {
            val error = ScopeInputError.AliasError.InvalidAliasFormat(
                alias = "InvalidAlias",
                expectedPattern = ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.LOWERCASE_WITH_HYPHENS,
            )

            error.shouldBeInstanceOf<ScopeInputError>()
            error.shouldBeInstanceOf<ScopeInputError.AliasError>()
            error.alias shouldBe "InvalidAlias"
            error.expectedPattern shouldBe ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.LOWERCASE_WITH_HYPHENS
        }

        "should support all AliasPatternType values" {
            val patternTypes = listOf(
                ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.LOWERCASE_WITH_HYPHENS,
                ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.ALPHANUMERIC,
                ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.ULID_LIKE,
                ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.CUSTOM_PATTERN,
            )

            patternTypes.forEach { patternType ->
                val error = ScopeInputError.AliasError.InvalidAliasFormat(
                    alias = "test-alias",
                    expectedPattern = patternType,
                )
                error.expectedPattern shouldBe patternType
            }
        }

        // Inheritance and type hierarchy tests
        "IdError subtypes should inherit from ScopeInputError" {
            val errors: List<ScopeInputError> = listOf(
                ScopeInputError.IdError.EmptyId,
                ScopeInputError.IdError.InvalidIdFormat("id", ScopeInputError.IdError.InvalidIdFormat.IdFormatType.ULID),
            )

            errors.forEach { error ->
                error.shouldBeInstanceOf<ScopeInputError>()
                error.shouldBeInstanceOf<ScopesError>()
            }
        }

        "TitleError subtypes should inherit from ScopeInputError" {
            val errors: List<ScopeInputError> = listOf(
                ScopeInputError.TitleError.EmptyTitle,
                ScopeInputError.TitleError.TitleTooShort(3),
                ScopeInputError.TitleError.TitleTooLong(200),
                ScopeInputError.TitleError.InvalidTitleFormat("title"),
            )

            errors.forEach { error ->
                error.shouldBeInstanceOf<ScopeInputError>()
                error.shouldBeInstanceOf<ScopesError>()
            }
        }

        "DescriptionError subtypes should inherit from ScopeInputError" {
            val error = ScopeInputError.DescriptionError.DescriptionTooLong(5000)

            error.shouldBeInstanceOf<ScopeInputError>()
            error.shouldBeInstanceOf<ScopesError>()
        }

        "AliasError subtypes should inherit from ScopeInputError" {
            val errors: List<ScopeInputError> = listOf(
                ScopeInputError.AliasError.EmptyAlias,
                ScopeInputError.AliasError.AliasTooShort(2),
                ScopeInputError.AliasError.AliasTooLong(64),
                ScopeInputError.AliasError.InvalidAliasFormat(
                    "alias",
                    ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.LOWERCASE_WITH_HYPHENS,
                ),
            )

            errors.forEach { error ->
                error.shouldBeInstanceOf<ScopeInputError>()
                error.shouldBeInstanceOf<ScopesError>()
            }
        }

        // Specific error scenarios
        "should handle various invalid ID formats" {
            val invalidIds = listOf(
                "" to "Empty ID",
                "123" to "Numeric ID when ULID expected",
                "not-a-ulid" to "Invalid ULID format",
                "550e8400-e29b-41d4-a716-446655440000" to "UUID when ULID expected",
            )

            invalidIds.forEach { (id, description) ->
                val error = ScopeInputError.IdError.InvalidIdFormat(
                    id = id,
                    expectedFormat = ScopeInputError.IdError.InvalidIdFormat.IdFormatType.ULID,
                )
                error.id shouldBe id
                // The description is just for test clarity
            }
        }

        "should handle various title validation errors" {
            val titleErrors = listOf(
                ScopeInputError.TitleError.EmptyTitle to "Empty title",
                ScopeInputError.TitleError.TitleTooShort(1) to "Single character minimum",
                ScopeInputError.TitleError.TitleTooLong(255) to "Database column limit",
                ScopeInputError.TitleError.InvalidTitleFormat("Line1\nLine2") to "Multi-line title",
            )

            titleErrors.forEach { (error, description) ->
                error.shouldBeInstanceOf<ScopeInputError.TitleError>()
                // The description is just for test clarity
            }
        }

        "should handle various alias format violations" {
            val aliasViolations = listOf(
                "UpperCase" to ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.LOWERCASE_WITH_HYPHENS,
                "has spaces" to ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.ALPHANUMERIC,
                "special@chars" to ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.ALPHANUMERIC,
                "trailing-" to ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.LOWERCASE_WITH_HYPHENS,
            )

            aliasViolations.forEach { (alias, pattern) ->
                val error = ScopeInputError.AliasError.InvalidAliasFormat(
                    alias = alias,
                    expectedPattern = pattern,
                )
                error.alias shouldBe alias
                error.expectedPattern shouldBe pattern
            }
        }
    })
