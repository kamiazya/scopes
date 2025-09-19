package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Tests for ScopeTitle value object.
 *
 * Business rules:
 * - Must not be blank
 * - Must be between 1 and 200 characters
 * - Must not contain newline or carriage return
 * - Normalizes internal whitespace
 * - Case-insensitive comparison available
 */
class ScopeTitleTest :
    StringSpec({

        "should create valid title with simple text" {
            val result = ScopeTitle.create("My Project")
            val title = result.shouldBeRight()
            title.value shouldBe "My Project"
            title.toString() shouldBe "My Project"
        }

        "should create valid title with numbers" {
            val result = ScopeTitle.create("Project 123")
            val title = result.shouldBeRight()
            title.value shouldBe "Project 123"
        }

        "should create valid title with special characters" {
            val result = ScopeTitle.create("Project: Phase-1 (MVP)")
            val title = result.shouldBeRight()
            title.value shouldBe "Project: Phase-1 (MVP)"
        }

        "should create valid title with unicode characters" {
            val result = ScopeTitle.create("プロジェクト 🚀")
            val title = result.shouldBeRight()
            title.value shouldBe "プロジェクト 🚀"
        }

        "should trim leading and trailing whitespace" {
            val variations = listOf(
                "  My Project  ",
                "\tMy Project\t",
            )

            variations.forEach { input ->
                val result = ScopeTitle.create(input)
                val title = result.shouldBeRight()
                title.value shouldBe "My Project"
            }
        }

        "should preserve internal whitespace in value" {
            val variations = listOf(
                "My  Project" to "My  Project",
                "My\t\tProject" to "My\t\tProject",
                "My   \t   Project" to "My   \t   Project",
            )

            variations.forEach { (input, expected) ->
                val result = ScopeTitle.create(input)
                val title = result.shouldBeRight()
                // The value property preserves internal whitespace, only trimming leading/trailing
                title.value shouldBe expected
            }
        }

        "should reject empty string" {
            val result = ScopeTitle.create("")
            result.shouldBeLeft()
            val error = result.leftOrNull()
            error shouldBe ScopeInputError.TitleError.EmptyTitle
        }

        "should reject blank string after trimming" {
            val blankStrings = listOf(
                "   ",
                "\t\t",
            )

            blankStrings.forEach { blank ->
                val result = ScopeTitle.create(blank)
                result.shouldBeLeft()
                val error = result.leftOrNull()
                error shouldBe ScopeInputError.TitleError.EmptyTitle
            }

            // Strings with newlines are rejected as InvalidTitleFormat
            val stringsWithNewlines = listOf(
                "\n\n",
                " \t\n ",
            )

            stringsWithNewlines.forEach { str ->
                val result = ScopeTitle.create(str)
                result.shouldBeLeft()
                val error = result.leftOrNull() as? ScopeInputError.TitleError.InvalidTitleFormat
                error?.title shouldBe str
            }
        }

        "should accept title at minimum length" {
            val result = ScopeTitle.create("A")
            val title = result.shouldBeRight()
            title.value shouldBe "A"
            title.value shouldHaveLength 1
        }

        "should accept title at maximum length" {
            val maxLengthTitle = "A".repeat(200)
            maxLengthTitle shouldHaveLength 200

            val result = ScopeTitle.create(maxLengthTitle)
            val title = result.shouldBeRight()
            title.value shouldBe maxLengthTitle
            title.value shouldHaveLength 200
        }

        "should reject title longer than maximum length" {
            val tooLongTitle = "A".repeat(201)
            val result = ScopeTitle.create(tooLongTitle)
            result.shouldBeLeft()
            val error = result.leftOrNull() as? ScopeInputError.TitleError.TitleTooLong
            error?.maxLength shouldBe 200
        }

        "should reject title with newline character" {
            val titlesWithNewline = listOf(
                "My\nProject",
                "My Project\n",
                "\nMy Project",
                "My\n\nProject",
            )

            titlesWithNewline.forEach { title ->
                val result = ScopeTitle.create(title)
                result.shouldBeLeft()
                val error = result.leftOrNull() as? ScopeInputError.TitleError.InvalidTitleFormat
                error?.title shouldBe title
            }
        }

        "should reject title with carriage return character" {
            val titlesWithCarriageReturn = listOf(
                "My\rProject",
                "My Project\r",
                "\rMy Project",
                "My\r\rProject",
            )

            titlesWithCarriageReturn.forEach { title ->
                val result = ScopeTitle.create(title)
                result.shouldBeLeft()
                val error = result.leftOrNull() as? ScopeInputError.TitleError.InvalidTitleFormat
                error?.title shouldBe title
            }
        }

        "should reject title with mixed newline and carriage return" {
            val result = ScopeTitle.create("My\r\nProject")
            result.shouldBeLeft()
            val error = result.leftOrNull() as? ScopeInputError.TitleError.InvalidTitleFormat
            error?.title shouldBe "My\r\nProject"
        }

        "should support case-insensitive comparison" {
            val title1 = ScopeTitle.create("My Project").getOrNull()!!
            val title2 = ScopeTitle.create("my project").getOrNull()!!
            val title3 = ScopeTitle.create("MY PROJECT").getOrNull()!!
            val title4 = ScopeTitle.create("mY pRoJeCt").getOrNull()!!

            // All should be equal when ignoring case
            title1.equalsIgnoreCase(title2) shouldBe true
            title1.equalsIgnoreCase(title3) shouldBe true
            title1.equalsIgnoreCase(title4) shouldBe true
            title2.equalsIgnoreCase(title3) shouldBe true
            title2.equalsIgnoreCase(title4) shouldBe true
            title3.equalsIgnoreCase(title4) shouldBe true
        }

        "should handle whitespace normalization in case-insensitive comparison" {
            val title1 = ScopeTitle.create("My  Project").getOrNull()!!
            val title2 = ScopeTitle.create("my\tproject").getOrNull()!!
            val title3 = ScopeTitle.create("MY   PROJECT").getOrNull()!!

            // All should be equal when ignoring case and normalizing whitespace
            title1.equalsIgnoreCase(title2) shouldBe true
            title1.equalsIgnoreCase(title3) shouldBe true
            title2.equalsIgnoreCase(title3) shouldBe true
        }

        "should differentiate titles that differ beyond case" {
            val title1 = ScopeTitle.create("My Project").getOrNull()!!
            val title2 = ScopeTitle.create("Your Project").getOrNull()!!
            val title3 = ScopeTitle.create("My Projects").getOrNull()!!

            title1.equalsIgnoreCase(title2) shouldBe false
            title1.equalsIgnoreCase(title3) shouldBe false
        }

        "should handle locale-invariant lowercase conversion" {
            // Test Turkish-I problem
            val title1 = ScopeTitle.create("I").getOrNull()!!
            val title2 = ScopeTitle.create("i").getOrNull()!!

            // Should be equal regardless of locale
            title1.equalsIgnoreCase(title2) shouldBe true

            // Test with Turkish specific characters
            val title3 = ScopeTitle.create("İstanbul").getOrNull()!!
            val title4 = ScopeTitle.create("istanbul").getOrNull()!!

            // Regular lowercase comparison (not Turkish-aware)
            title3.value shouldBe "İstanbul"
            title4.value shouldBe "istanbul"
        }

        "should maintain immutability" {
            val title = "My Project"
            val scopeTitle = ScopeTitle.create(title).getOrNull()!!

            // Value should be immutable
            scopeTitle.value shouldBe title

            // toString should return the same value
            scopeTitle.toString() shouldBe title

            // Creating another with same value should be equal
            val scopeTitle2 = ScopeTitle.create(title).getOrNull()!!
            scopeTitle shouldBe scopeTitle2
            scopeTitle.value shouldBe scopeTitle2.value
        }

        "should handle inline value class behavior" {
            val title = "My Project"
            val scopeTitle = ScopeTitle.create(title).getOrNull()!!

            // Inline value class should unwrap to its value in most contexts
            scopeTitle.value shouldBe title

            // But maintain type safety
            val anotherScopeTitle = ScopeTitle.create(title).getOrNull()!!
            scopeTitle shouldBe anotherScopeTitle
        }

        "should handle realistic title scenarios" {
            val validTitles = listOf(
                "Sprint Planning",
                "Q4 2023 Goals",
                "Feature: User Authentication",
                "Bug #1234 - Login issue",
                "[URGENT] Production hotfix",
                "Meeting notes - 2023/10/15",
                "TODO: Refactor database layer",
                "版本2.0 リリース準備",
                "🚀 Launch preparation",
            )

            validTitles.forEach { title ->
                val result = ScopeTitle.create(title)
                result.shouldBeRight()
                result.getOrNull()?.value shouldBe title
            }
        }

        "should handle property-based testing for valid titles" {
            checkAll(Arb.string(1..200).filter { it.isNotBlank() && !it.contains('\n') && !it.contains('\r') }) { str ->
                val result = ScopeTitle.create(str)
                val title = result.shouldBeRight()

                // Value should be trimmed but preserve internal whitespace
                title.value shouldBe str.trim()

                // toString should return the value
                title.toString() shouldBe title.value
            }
        }

        "should handle edge cases in string processing" {
            val validCases = mapOf(
                " A " to "A", // Single char with spaces
                "\tB\t" to "B", // Single char with tabs
                "C  D" to "C  D", // Double space preserved
                "E\t\tF" to "E\t\tF", // Multiple tabs preserved
                " G   H " to "G   H", // Mixed whitespace preserved
                "K".repeat(200) to "K".repeat(200), // Max length
                "  " + "L".repeat(198) + "  " to "L".repeat(198), // Max after trim
            )

            validCases.forEach { (input, expected) ->
                val result = ScopeTitle.create(input)
                result.shouldBeRight()
                result.getOrNull()?.value shouldBe expected
            }

            // Test case with newline should be rejected
            val resultWithNewline = ScopeTitle.create("I\n \tJ")
            resultWithNewline.shouldBeLeft()
            val error = resultWithNewline.leftOrNull() as? ScopeInputError.TitleError.InvalidTitleFormat
            error?.title shouldBe "I\n \tJ"
        }

        "should verify toString implementation" {
            val title = "My Project"
            val scopeTitle = ScopeTitle.create(title).getOrNull()!!

            scopeTitle.toString() shouldBe title
            scopeTitle.value shouldBe title
        }

        "should handle control characters properly" {
            val invalidStrings = listOf(
                "My\u0000Project", // null character
                "My\u0001Project", // SOH character
                "My\u0008Project", // backspace
                "My\u001BProject", // escape
            )

            // These should create successfully as they don't contain \n or \r
            invalidStrings.forEach { str ->
                val result = ScopeTitle.create(str)
                result.shouldBeRight()
                // Control characters are preserved but whitespace normalized
                val expected = str.trim().replace(Regex("\\s+"), " ")
                result.getOrNull()?.value shouldBe expected
            }
        }

        "should create multiple unique titles" {
            val titles = listOf(
                "Project Alpha",
                "Project Beta",
                "Project Gamma",
                "Task 1",
                "Task 2",
            )

            val createdTitles = mutableSetOf<String>()
            titles.forEach { title ->
                val result = ScopeTitle.create(title)
                val scopeTitle = result.shouldBeRight()
                createdTitles.add(scopeTitle.value) shouldBe true
            }

            createdTitles.size shouldBe titles.size
        }
    })
