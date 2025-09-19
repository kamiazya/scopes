package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Tests for ScopeDescription value object.
 *
 * Business rules:
 * - Can be null (optional field)
 * - Max length is 1000 characters
 * - Blank strings become null
 * - Trims leading and trailing whitespace
 * - Preserves internal whitespace and newlines
 */
class ScopeDescriptionTest :
    StringSpec({

        "should create valid description with simple text" {
            val result = ScopeDescription.create("This is a simple description")
            val description = result.shouldBeRight()
            description.shouldNotBeNull()
            description.value shouldBe "This is a simple description"
            description.toString() shouldBe "This is a simple description"
        }

        "should create valid description with numbers" {
            val result = ScopeDescription.create("Version 2.0 features")
            val description = result.shouldBeRight()
            description.shouldNotBeNull()
            description.value shouldBe "Version 2.0 features"
        }

        "should create valid description with special characters" {
            val result = ScopeDescription.create("Features: Auth, DB (PostgreSQL), API v3")
            val description = result.shouldBeRight()
            description.shouldNotBeNull()
            description.value shouldBe "Features: Auth, DB (PostgreSQL), API v3"
        }

        "should create valid description with unicode characters" {
            val result = ScopeDescription.create("詳細説明 🚀 International project")
            val description = result.shouldBeRight()
            description.shouldNotBeNull()
            description.value shouldBe "詳細説明 🚀 International project"
        }

        "should handle null input by returning null" {
            val result = ScopeDescription.create(null)
            val description = result.shouldBeRight()
            description.shouldBeNull()
        }

        "should handle empty string by returning null" {
            val result = ScopeDescription.create("")
            val description = result.shouldBeRight()
            description.shouldBeNull()
        }

        "should handle blank string by returning null" {
            val blankStrings = listOf(
                " ",
                "   ",
                "\t",
                "\n",
                "\r\n",
                " \t\n\r ",
            )

            blankStrings.forEach { blank ->
                val result = ScopeDescription.create(blank)
                val description = result.shouldBeRight()
                description.shouldBeNull()
            }
        }

        "should trim leading and trailing whitespace" {
            val variations = listOf(
                "  Description  " to "Description",
                "\tDescription\t" to "Description",
                "\nDescription\n" to "Description",
                " \t\n Description \n\t " to "Description",
            )

            variations.forEach { (input, expected) ->
                val result = ScopeDescription.create(input)
                val description = result.shouldBeRight()
                description.shouldNotBeNull()
                description.value shouldBe expected
            }
        }

        "should preserve internal whitespace" {
            val descriptions = listOf(
                "Line 1  Line 2" to "Line 1  Line 2",
                "Tab\t\tSeparated" to "Tab\t\tSeparated",
                "Mixed   \t   Spaces" to "Mixed   \t   Spaces",
            )

            descriptions.forEach { (input, expected) ->
                val result = ScopeDescription.create(input)
                val description = result.shouldBeRight()
                description.shouldNotBeNull()
                description.value shouldBe expected
            }
        }

        "should preserve newlines" {
            val multilineDesc = """
            Feature description:
            - Authentication
            - Authorization
            - User management
            """.trimIndent()

            val result = ScopeDescription.create(multilineDesc)
            val description = result.shouldBeRight()
            description.shouldNotBeNull()
            description.value shouldBe multilineDesc
        }

        "should accept description at maximum length" {
            val maxLengthDesc = "A".repeat(1000)
            maxLengthDesc shouldHaveLength 1000

            val result = ScopeDescription.create(maxLengthDesc)
            val description = result.shouldBeRight()
            description.shouldNotBeNull()
            description.value shouldBe maxLengthDesc
            description.value shouldHaveLength 1000
        }

        "should reject description longer than maximum length" {
            val tooLongDesc = "A".repeat(1001)
            val result = ScopeDescription.create(tooLongDesc)
            result.shouldBeLeft()
            val error = result.leftOrNull() as? ScopeInputError.DescriptionError.DescriptionTooLong
            error?.maxLength shouldBe 1000
        }

        "should handle realistic description scenarios" {
            val validDescriptions = listOf(
                "This task involves refactoring the authentication module",
                "Bug fix for issue #1234 - Login form validation",
                """
            Multi-line description:
            1. Implement user authentication
            2. Add password reset functionality
            3. Integrate with LDAP
                """.trimIndent(),
                "TODO: Review security implications before deployment",
                "⚠️ URGENT: Fix critical production bug",
                "See documentation at https://example.com/docs",
            )

            validDescriptions.forEach { desc ->
                val result = ScopeDescription.create(desc)
                val description = result.shouldBeRight()
                description.shouldNotBeNull()
                description.value shouldBe desc
            }
        }

        "should handle edge cases" {
            val edgeCases = mapOf(
                " A " to "A", // Single char with spaces
                "\tB\t" to "B", // Single char with tabs
                " " + "C".repeat(1000) + " " to "C".repeat(1000), // Max length with spaces
                "  " + "D".repeat(998) + "  " to "D".repeat(998), // Almost max after trim
            )

            edgeCases.forEach { (input, expected) ->
                val result = ScopeDescription.create(input)
                val description = result.shouldBeRight()
                description.shouldNotBeNull()
                description.value shouldBe expected
            }
        }

        "should verify toString implementation" {
            val text = "Sample description"
            val description = ScopeDescription.create(text).getOrNull()!!

            description.toString() shouldBe text
            description.value shouldBe text
        }

        "should maintain immutability" {
            val text = "Immutable description"
            val description1 = ScopeDescription.create(text).getOrNull()!!
            val description2 = ScopeDescription.create(text).getOrNull()!!

            description1 shouldBe description2
            description1.value shouldBe description2.value
        }

        "should handle property-based testing for valid descriptions" {
            checkAll(Arb.string(0..1000)) { str ->
                val result = ScopeDescription.create(str)
                val description = result.shouldBeRight()

                val trimmed = str.trim()
                if (trimmed.isEmpty()) {
                    description.shouldBeNull()
                } else {
                    description.shouldNotBeNull()
                    description.value shouldBe trimmed
                    description.toString() shouldBe trimmed
                }
            }
        }

        "should handle control characters and special content" {
            val specialCases = listOf(
                "Contains\u0000null", // null character
                "Contains\u0001SOH", // SOH character
                "Contains\u0008backspace", // backspace
                "Contains\u001Bescape", // escape
                "Line1\nLine2\nLine3", // Multiple newlines
                "Tab1\tTab2\tTab3", // Multiple tabs
                "\r\nWindows\r\nLineEndings\r\n", // Windows line endings
            )

            specialCases.forEach { content ->
                val result = ScopeDescription.create(content)
                val description = result.shouldBeRight()
                description.shouldNotBeNull()
                description.value shouldBe content.trim()
            }
        }

        "should create multiple unique descriptions" {
            val descriptions = listOf(
                "Description 1",
                "Description 2",
                "Another description",
                "Yet another one",
                "Final description",
            )

            val createdDescriptions = mutableSetOf<String>()
            descriptions.forEach { desc ->
                val result = ScopeDescription.create(desc)
                val description = result.shouldBeRight()
                description.shouldNotBeNull()
                createdDescriptions.add(description.value) shouldBe true
            }

            createdDescriptions.size shouldBe descriptions.size
        }
    })
