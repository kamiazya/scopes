package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Tests for ContextViewDescription value object.
 *
 * Business rules:
 * - Must not be empty or blank
 * - Must be between 1 and 500 characters after trimming
 * - Supports multi-line descriptions
 * - Preserves internal whitespace after trimming
 */
class ContextViewDescriptionTest :
    StringSpec({

        "should create valid description" {
            val description = "This is a context for high-priority work items"
            val result = ContextViewDescription.create(description)
            val desc = result.shouldBeRight()
            desc.value shouldBe description
            desc.toString() shouldBe description
        }

        "should trim whitespace from input" {
            val result = ContextViewDescription.create("  Description with spaces  ")
            val desc = result.shouldBeRight()
            desc.value shouldBe "Description with spaces"
        }

        "should accept multi-line description" {
            val multiLine = """
            This is a multi-line description.
            It contains multiple lines of text.
            Each line provides more detail.
            """.trimIndent()

            val result = ContextViewDescription.create(multiLine)
            val desc = result.shouldBeRight()
            desc.value shouldBe multiLine
        }

        "should preserve internal whitespace" {
            val spaced = "This   has    multiple     spaces"
            val result = ContextViewDescription.create(spaced)
            val desc = result.shouldBeRight()
            desc.value shouldBe spaced
        }

        "should reject empty string" {
            val result = ContextViewDescription.create("")
            result.shouldBeLeft()
            result.leftOrNull() shouldBe ContextError.EmptyDescription
        }

        "should reject blank string" {
            val result = ContextViewDescription.create("   \n\t  ")
            result.shouldBeLeft()
            result.leftOrNull() shouldBe ContextError.EmptyDescription
        }

        "should accept single character description" {
            val result = ContextViewDescription.create("A")
            val desc = result.shouldBeRight()
            desc.value shouldBe "A"
            desc.value.length shouldBe 1
        }

        "should accept description at maximum length" {
            val maxDesc = "a".repeat(500)
            val result = ContextViewDescription.create(maxDesc)
            val desc = result.shouldBeRight()
            desc.value shouldBe maxDesc
            desc.value.length shouldBe 500
        }

        "should reject description that is too long" {
            val longDesc = "a".repeat(501)
            val result = ContextViewDescription.create(longDesc)
            result.shouldBeLeft()
            result.leftOrNull() shouldBe ContextError.DescriptionTooLong(maximumLength = 500)
        }

        "should handle edge case at boundaries" {
            // Just under max
            val desc499 = "a".repeat(499)
            ContextViewDescription.create(desc499).shouldBeRight()

            // At max
            val desc500 = "a".repeat(500)
            ContextViewDescription.create(desc500).shouldBeRight()

            // Just over max
            val desc501 = "a".repeat(501)
            ContextViewDescription.create(desc501).shouldBeLeft()
        }

        "should accept descriptions with special characters" {
            val specialChars = """
            Description with special chars: @#$%^&*()_+-={}[]|\:";'<>?,./
            Unicode: ñáéíóú 中文 日本語 한국어 العربية עברית
            Emojis: 😀 🚀 ⭐ ❤️ 🔥
            """.trimIndent()

            val result = ContextViewDescription.create(specialChars)
            val desc = result.shouldBeRight()
            desc.value shouldBe specialChars
        }

        "should handle descriptions with only newlines after trimming as empty" {
            val newlines = "\n\n\n\n"
            val result = ContextViewDescription.create(newlines)
            result.shouldBeLeft()
            result.leftOrNull() shouldBe ContextError.EmptyDescription
        }

        "should preserve line breaks in multi-line descriptions" {
            val multiLine = "Line 1\nLine 2\n\nLine 4 (after empty line)"
            val result = ContextViewDescription.create(multiLine)
            val desc = result.shouldBeRight()
            desc.value shouldBe multiLine
            desc.value.lines().size shouldBe 4
        }

        "should verify toString returns the value" {
            val description = "Test description"
            val result = ContextViewDescription.create(description)
            val desc = result.shouldBeRight()
            desc.toString() shouldBe description
            desc.toString() shouldBe desc.value
        }

        // Property-based testing
        "should always trim input strings" {
            checkAll(Arb.string(0..600)) { input ->
                val result = ContextViewDescription.create(input)
                result.fold(
                    { true }, // Error case is valid
                    { desc -> desc.value == input.trim() },
                )
            }
        }

        "should handle realistic context descriptions" {
            val realDescriptions = listOf(
                "All work items assigned to the current sprint",
                "High-priority bugs that need immediate attention",
                "Features planned for Q2 2025 release",
                "Tasks blocked by external dependencies",
                "Code review items waiting for approval",
                """
            This context shows all development tasks that are:
            - Assigned to the frontend team
            - Have priority >= medium
            - Are not yet completed
                """.trimIndent(),
                "简单的中文描述",
                "Personal projects and learning tasks",
            )

            realDescriptions.forEach { realDesc ->
                val result = ContextViewDescription.create(realDesc)
                val desc = result.shouldBeRight()
                desc.value shouldBe realDesc.trim()
            }
        }

        "should enforce minimum length after trimming" {
            // Strings that become empty after trimming
            val spacesOnly = "   "
            val result = ContextViewDescription.create(spacesOnly)
            result.shouldBeLeft()
            result.leftOrNull() shouldBe ContextError.EmptyDescription
        }

        "should handle descriptions with tabs and mixed whitespace" {
            val mixed = "\t\tTabbed description\n  with mixed   spacing\t"
            val result = ContextViewDescription.create(mixed)
            val desc = result.shouldBeRight()
            desc.value shouldBe "Tabbed description\n  with mixed   spacing"
        }
    })
