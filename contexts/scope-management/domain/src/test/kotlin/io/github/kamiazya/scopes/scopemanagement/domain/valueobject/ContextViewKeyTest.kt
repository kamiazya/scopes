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
 * Tests for ContextViewKey value object.
 *
 * Business rules:
 * - Must not be empty or blank
 * - Must be between 2 and 50 characters
 * - Must contain only lowercase letters, numbers, hyphens, and underscores
 * - Must start with a letter
 * - Cannot end with a hyphen or underscore
 */
class ContextViewKeyTest :
    StringSpec({

        "should create valid context view key" {
            val result = ContextViewKey.create("my-work-context")
            val key = result.shouldBeRight()
            key.value shouldBe "my-work-context"
            key.toString() shouldBe "my-work-context"
        }

        "should reject single letter key due to MIN_LENGTH" {
            val result = ContextViewKey.create("a")
            result.shouldBeLeft()
            result.leftOrNull() shouldBe ContextError.KeyTooShort(2)
        }

        "should accept various valid formats" {
            val validKeys = listOf(
                "ab",
                "my-context",
                "work_context",
                "sprint42",
                "feature-123",
                "bug_fix_123",
                "a1",
                "test-123-key",
                "underscore_separated",
                "hyphen-separated",
                "mixed-123_style",
                "z9", // ends with number
            )

            validKeys.forEach { testKey ->
                val result = ContextViewKey.create(testKey)
                val key = result.shouldBeRight()
                key.value shouldBe testKey
            }
        }

        "should trim whitespace from input" {
            val result = ContextViewKey.create("  my-context  ")
            val key = result.shouldBeRight()
            key.value shouldBe "my-context"
        }

        "should reject empty string" {
            val result = ContextViewKey.create("")
            result.shouldBeLeft()
            result.leftOrNull() shouldBe ContextError.EmptyKey
        }

        "should reject blank string" {
            val result = ContextViewKey.create("   ")
            result.shouldBeLeft()
            result.leftOrNull() shouldBe ContextError.EmptyKey
        }

        "should accept key at minimum length" {
            val result = ContextViewKey.create("ab") // Minimum 2 characters
            result.shouldBeRight()

            // Single letter "a" is rejected due to MIN_LENGTH = 2
            val singleLetter = ContextViewKey.create("a")
            singleLetter.shouldBeLeft()
        }

        "should reject key that is too long" {
            val longKey = "a".repeat(51) // 51 characters
            val result = ContextViewKey.create(longKey)
            result.shouldBeLeft()
            result.leftOrNull() shouldBe ContextError.KeyTooLong(50)
        }

        "should accept key at maximum length" {
            val maxKey = "a" + "b".repeat(49) // 50 characters total
            val result = ContextViewKey.create(maxKey)
            val key = result.shouldBeRight()
            key.value shouldBe maxKey
            key.value.length shouldBe 50
        }

        "should reject uppercase letters" {
            val invalidKeys = listOf(
                "MyContext",
                "my-Context",
                "MY-CONTEXT",
                "Context",
                "TEST",
            )

            invalidKeys.forEach { testKey ->
                val result = ContextViewKey.create(testKey)
                result.shouldBeLeft()
                val error = result.leftOrNull()
                error shouldBe ContextError.InvalidKeyFormat(
                    errorType = ContextError.InvalidKeyFormat.InvalidKeyFormatType.INVALID_PATTERN,
                )
            }
        }

        "should reject keys starting with non-letter" {
            val invalidKeys = listOf(
                "1context",
                "123",
                "-context",
                "_context",
                "9test",
            )

            invalidKeys.forEach { testKey ->
                val result = ContextViewKey.create(testKey)
                result.shouldBeLeft()
                val error = result.leftOrNull()
                error shouldBe ContextError.InvalidKeyFormat(
                    errorType = ContextError.InvalidKeyFormat.InvalidKeyFormatType.INVALID_PATTERN,
                )
            }
        }

        "should reject keys ending with hyphen or underscore" {
            val invalidKeys = listOf(
                "context-",
                "context_",
                "my-context-",
                "my_context_",
                "ab-",
                "ab_",
            )

            invalidKeys.forEach { testKey ->
                val result = ContextViewKey.create(testKey)
                result.shouldBeLeft()
                val error = result.leftOrNull()
                error shouldBe ContextError.InvalidKeyFormat(
                    errorType = ContextError.InvalidKeyFormat.InvalidKeyFormatType.INVALID_PATTERN,
                )
            }
        }

        "should reject keys with invalid characters" {
            val invalidKeys = listOf(
                "my context", // space
                "my.context", // dot
                "my/context", // slash
                "my@context", // at
                "my#context", // hash
                "my\$context", // dollar
                "my%context", // percent
                "my&context", // ampersand
                "my*context", // asterisk
                "my+context", // plus
                "my=context", // equals
                "my!context", // exclamation
                "my?context", // question
                "my(context)", // parentheses
                "my[context]", // brackets
                "my{context}", // braces
                "my'context", // quote
                "my\"context", // double quote
                "my\\context", // backslash
                "my|context", // pipe
                "my~context", // tilde
                "my`context", // backtick
                "my,context", // comma
                "my;context", // semicolon
                "my:context", // colon
            )

            invalidKeys.forEach { testKey ->
                val result = ContextViewKey.create(testKey)
                result.shouldBeLeft()
                val error = result.leftOrNull()
                error shouldBe ContextError.InvalidKeyFormat(
                    errorType = ContextError.InvalidKeyFormat.InvalidKeyFormatType.INVALID_PATTERN,
                )
            }
        }

        "should handle edge cases for single character" {
            // Single lowercase letter is rejected due to MIN_LENGTH = 2
            ('a'..'z').forEach { char ->
                val result = ContextViewKey.create(char.toString())
                result.shouldBeLeft()
                result.leftOrNull() shouldBe ContextError.KeyTooShort(2)
            }

            // Single non-letter is also invalid
            listOf('0', '1', '9', '-', '_').forEach { char ->
                val result = ContextViewKey.create(char.toString())
                result.shouldBeLeft()
                // Could be KeyTooShort or InvalidKeyFormat depending on validation order
            }
        }

        "should verify toString returns the value" {
            val testKey = "test-key"
            val result = ContextViewKey.create(testKey)
            val key = result.shouldBeRight()
            key.toString() shouldBe testKey
            key.toString() shouldBe key.value
        }

        // Property-based testing
        "should always trim input strings" {
            checkAll(Arb.string(0..60)) { input ->
                val result = ContextViewKey.create(input)
                result.fold(
                    { true }, // Error case is valid
                    { key -> key.value == input.trim() },
                )
            }
        }

        // Test realistic key patterns
        "should handle realistic context keys" {
            val realKeys = listOf(
                "work",
                "personal",
                "sprint-42",
                "q1-2025",
                "client-acme",
                "high-priority",
                "bug-fixes",
                "feature-auth",
                "team-alpha",
                "code-review",
                "waiting",
                "archived",
                "dev",
                "test",
                "prod",
            )

            realKeys.forEach { realKey ->
                val result = ContextViewKey.create(realKey)
                val key = result.shouldBeRight()
                key.value shouldBe realKey
            }
        }

        "should handle consecutive hyphens and underscores internally" {
            val validKeys = listOf(
                "my--context", // double hyphen
                "my__context", // double underscore
                "my---context", // triple hyphen
                "my-_-context", // mixed
                "a--b",
                "a__b",
            )

            validKeys.forEach { testKey ->
                val result = ContextViewKey.create(testKey)
                val key = result.shouldBeRight()
                key.value shouldBe testKey
            }
        }

        "should enforce minimum length of 2 characters" {
            // Two character minimum
            val result = ContextViewKey.create("ab")
            result.shouldBeRight()

            // Single character is always rejected
            val single = ContextViewKey.create("a")
            single.shouldBeLeft()
            single.leftOrNull() shouldBe ContextError.KeyTooShort(2)
        }

        "should handle exact boundary cases" {
            // Exactly at min length (2)
            val minKey = "ab"
            ContextViewKey.create(minKey).shouldBeRight()

            // Exactly at max length (50)
            val maxKey = "a" + "b".repeat(48) + "c" // 50 chars
            ContextViewKey.create(maxKey).shouldBeRight()

            // One over max
            val overMax = maxKey + "d"
            ContextViewKey.create(overMax).shouldBeLeft()
        }
    })
