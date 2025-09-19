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
 * Tests for ContextViewName value object.
 */
class ContextViewNameTest :
    StringSpec({

        "should create valid context view name" {
            val result = ContextViewName.create("My Work Context")
            val name = result.shouldBeRight()
            name.value shouldBe "My Work Context"
            name.toString() shouldBe "My Work Context"
        }

        "should trim whitespace from input" {
            val result = ContextViewName.create("  Trimmed Context Name  ")
            val name = result.shouldBeRight()
            name.value shouldBe "Trimmed Context Name"
        }

        "should reject empty string" {
            val result = ContextViewName.create("")
            result.shouldBeLeft()
            result.leftOrNull() shouldBe ContextError.EmptyName
        }

        "should reject blank string" {
            val result = ContextViewName.create("   ")
            result.shouldBeLeft()
            result.leftOrNull() shouldBe ContextError.EmptyName
        }

        "should reject name that is too long" {
            val longName = "a".repeat(ContextViewName.MAX_LENGTH + 1)
            val result = ContextViewName.create(longName)
            result.shouldBeLeft()
            val error = result.leftOrNull()
            error shouldBe ContextError.NameTooLong(maximumLength = ContextViewName.MAX_LENGTH)
        }

        "should accept name at maximum length" {
            val maxName = "a".repeat(ContextViewName.MAX_LENGTH)
            val result = ContextViewName.create(maxName)
            val name = result.shouldBeRight()
            name.value shouldBe maxName
            name.value.length shouldBe ContextViewName.MAX_LENGTH
        }

        "should accept single character name" {
            val result = ContextViewName.create("A")
            val name = result.shouldBeRight()
            name.value shouldBe "A"
        }

        "should preserve case in name" {
            val testCases = listOf(
                "My Context",
                "my context",
                "MY CONTEXT",
                "MyContext",
                "myContext",
                "MYCONTEXT",
            )

            testCases.forEach { testName ->
                val result = ContextViewName.create(testName)
                val name = result.shouldBeRight()
                name.value shouldBe testName
            }
        }

        "should accept names with special characters" {
            val testCases = listOf(
                "Work-Related",
                "Personal & Projects",
                "Client #1",
                "Sprint @42",
                "Dev/Test Context",
                "High-Priority (Important)",
                "Context: Frontend",
                "Tasks [In Progress]",
                "Feature|Bug Fixes",
                "Project \"Alpha\"",
                "Ελληνικά Context", // Greek
                "日本語コンテキスト", // Japanese
                "😊 Happy Context", // Emoji
            )

            testCases.forEach { testName ->
                val result = ContextViewName.create(testName)
                val name = result.shouldBeRight()
                name.value shouldBe testName
            }
        }

        "should handle whitespace variations" {
            val testCases = listOf(
                "  Leading spaces" to "Leading spaces",
                "Trailing spaces  " to "Trailing spaces",
                "  Both sides  " to "Both sides",
                "Multiple   spaces" to "Multiple   spaces", // Internal spaces preserved
                "\tTab prefix" to "Tab prefix",
                "Tab suffix\t" to "Tab suffix",
                "\nNewline prefix" to "Newline prefix",
            )

            testCases.forEach { (input, expected) ->
                val result = ContextViewName.create(input)
                val name = result.shouldBeRight()
                name.value shouldBe expected
            }
        }

        "should handle multiline input by trimming" {
            val multiline = """
            Multi
            Line
            Context
            """.trimIndent()

            val result = ContextViewName.create(multiline)
            val name = result.shouldBeRight()
            // Multiline is allowed, trimming only affects leading/trailing whitespace
            name.value shouldBe multiline
        }

        "should verify MAX_LENGTH constant" {
            ContextViewName.MAX_LENGTH shouldBe 100
        }

        // Property-based testing
        "should always trim input strings" {
            checkAll(Arb.string(0..150)) { input ->
                val result = ContextViewName.create(input)
                result.fold(
                    { true }, // Error case is valid
                    { name -> name.value == input.trim() },
                )
            }
        }

        "should reject all strings longer than MAX_LENGTH after trimming" {
            checkAll(Arb.string(ContextViewName.MAX_LENGTH + 1..200)) { input ->
                if (input.trim().length > ContextViewName.MAX_LENGTH) {
                    val result = ContextViewName.create(input)
                    result.shouldBeLeft()
                    result.leftOrNull() shouldBe ContextError.NameTooLong(maximumLength = ContextViewName.MAX_LENGTH)
                }
            }
        }

        // Test realistic context names
        "should handle realistic context names" {
            val realNames = listOf(
                "Active Development",
                "Sprint 42 - Authentication",
                "Personal Projects",
                "Client Work - Acme Corp",
                "Bug Fixes (High Priority)",
                "Research & Development",
                "Q1 2025 Goals",
                "Team Alpha Tasks",
                "Code Review Queue",
                "Waiting for Feedback",
                "Archived - Old Sprint",
                "🚀 Launch Preparation",
            )

            realNames.forEach { realName ->
                val result = ContextViewName.create(realName)
                val name = result.shouldBeRight()
                name.value shouldBe realName
            }
        }

        // Edge cases
        "should handle only whitespace characters" {
            val whitespaceInputs = listOf(
                " ",
                "  ",
                "\t",
                "\n",
                "\r",
                " \t \n \r ",
                "　", // Full-width space
            )

            whitespaceInputs.forEach { input ->
                val result = ContextViewName.create(input)
                result.shouldBeLeft()
                result.leftOrNull() shouldBe ContextError.EmptyName
            }
        }

        "should verify toString returns the value" {
            val testName = "Test Context"
            val result = ContextViewName.create(testName)
            val name = result.shouldBeRight()
            name.toString() shouldBe testName
            name.toString() shouldBe name.value
        }
    })
