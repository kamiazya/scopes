package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.github.kamiazya.scopes.scopemanagement.domain.error.AspectKeyError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Tests for AspectKey value object.
 *
 * Business rules:
 * - Min length: 1 character
 * - Max length: 50 characters
 * - Must start with a letter (a-z or A-Z)
 * - Can contain letters, numbers, hyphens, and underscores
 * - No whitespace allowed
 */
class AspectKeyTest :
    StringSpec({

        "should create valid aspect key with letters only" {
            val result = AspectKey.create("priority")
            val key = result.shouldBeRight()
            key.value shouldBe "priority"
            key.toString() shouldBe "priority"
        }

        "should create valid aspect key with uppercase letters" {
            val result = AspectKey.create("STATUS")
            val key = result.shouldBeRight()
            key.value shouldBe "STATUS"
        }

        "should create valid aspect key with mixed case" {
            val result = AspectKey.create("CreatedAt")
            val key = result.shouldBeRight()
            key.value shouldBe "CreatedAt"
        }

        "should create valid aspect key with numbers" {
            val result = AspectKey.create("phase2")
            val key = result.shouldBeRight()
            key.value shouldBe "phase2"
        }

        "should create valid aspect key with hyphens" {
            val result = AspectKey.create("due-date")
            val key = result.shouldBeRight()
            key.value shouldBe "due-date"
        }

        "should create valid aspect key with underscores" {
            val result = AspectKey.create("created_by")
            val key = result.shouldBeRight()
            key.value shouldBe "created_by"
        }

        "should create valid aspect key with mixed special chars" {
            val result = AspectKey.create("api-version_2")
            val key = result.shouldBeRight()
            key.value shouldBe "api-version_2"
        }

        "should accept key at minimum length" {
            val result = AspectKey.create("a")
            val key = result.shouldBeRight()
            key.value shouldBe "a"
        }

        "should accept key at maximum length" {
            val maxKey = "a" + "b".repeat(49) // 50 chars total
            val result = AspectKey.create(maxKey)
            val key = result.shouldBeRight()
            key.value shouldBe maxKey
            key.value.length shouldBe 50
        }

        "should reject empty string" {
            val result = AspectKey.create("")
            result.shouldBeLeft()
            val error = result.leftOrNull()
            error.shouldBeInstanceOf<AspectKeyError.EmptyKey>()
        }

        "should reject blank string" {
            val blankStrings = listOf(" ", "   ", "\t", "\n", " \t\n ")

            blankStrings.forEach { blank ->
                val result = AspectKey.create(blank)
                result.shouldBeLeft()
                val error = result.leftOrNull()
                error.shouldBeInstanceOf<AspectKeyError.EmptyKey>()
            }
        }

        "should reject key starting with number" {
            val result = AspectKey.create("2phase")
            result.shouldBeLeft()
            val error = result.leftOrNull()
            error.shouldBeInstanceOf<AspectKeyError.InvalidFormat>()
        }

        "should reject key starting with hyphen" {
            val result = AspectKey.create("-priority")
            result.shouldBeLeft()
            val error = result.leftOrNull()
            error.shouldBeInstanceOf<AspectKeyError.InvalidFormat>()
        }

        "should reject key starting with underscore" {
            val result = AspectKey.create("_status")
            result.shouldBeLeft()
            val error = result.leftOrNull()
            error.shouldBeInstanceOf<AspectKeyError.InvalidFormat>()
        }

        "should reject key with spaces" {
            val keysWithSpaces = listOf(
                "due date",
                "created at",
                "priority level",
                "has space",
            )

            keysWithSpaces.forEach { key ->
                val result = AspectKey.create(key)
                result.shouldBeLeft()
                val error = result.leftOrNull()
                error.shouldBeInstanceOf<AspectKeyError.InvalidFormat>()
            }
        }

        "should reject key with special characters" {
            val invalidKeys = listOf(
                "priority!",
                "status@",
                "type#",
                "value$",
                "percent%",
                "and&",
                "star*",
                "plus+",
                "equals=",
                "bracket[",
                "paren(",
                "dot.com",
                "comma,separated",
                "semi;colon",
                "colon:value",
                "quote'test",
                "double\"quote",
                "back`tick",
                "tilde~value",
                "pipe|symbol",
                "backslash\\path",
                "forward/slash",
                "question?",
                "less<than",
                "greater>than",
            )

            invalidKeys.forEach { key ->
                val result = AspectKey.create(key)
                result.shouldBeLeft()
                val error = result.leftOrNull()
                error.shouldBeInstanceOf<AspectKeyError.InvalidFormat>()
            }
        }

        "should reject key longer than maximum length" {
            val tooLong = "a".repeat(51)
            val result = AspectKey.create(tooLong)
            result.shouldBeLeft()
            val error = result.leftOrNull()
            error.shouldBeInstanceOf<AspectKeyError.TooLong>()
            error.actualLength shouldBe 51
            error.maxLength shouldBe 50
        }

        "should handle realistic aspect key scenarios" {
            val validKeys = listOf(
                "priority",
                "status",
                "assignee",
                "dueDate",
                "createdAt",
                "updatedAt",
                "type",
                "component",
                "version",
                "milestone",
                "sprint",
                "epic",
                "storyPoints",
                "timeEstimate",
                "actualTime",
                "reviewer",
                "approver",
                "category",
                "subCategory",
                "risk_level",
                "test-coverage",
                "build_status",
                "deploy-env",
                "api_version",
                "schema_ver",
            )

            validKeys.forEach { key ->
                val result = AspectKey.create(key)
                val aspectKey = result.shouldBeRight()
                aspectKey.value shouldBe key
            }
        }

        "should handle edge cases" {
            val edgeCases = mapOf(
                "a" to "a", // Single char (min length)
                "A" to "A", // Single uppercase
                "a" + "2".repeat(49) to "a" + "2".repeat(49), // Max length with numbers
                "z_-_-_-_-" to "z_-_-_-_-", // Alternating special chars
                "CamelCaseKey" to "CamelCaseKey", // Camel case
                "SCREAMING_SNAKE" to "SCREAMING_SNAKE", // Screaming snake case
                "kebab-case-key" to "kebab-case-key", // Kebab case
                "snake_case_key" to "snake_case_key", // Snake case
            )

            edgeCases.forEach { (input, expected) ->
                val result = AspectKey.create(input)
                val key = result.shouldBeRight()
                key.value shouldBe expected
            }
        }

        "should verify toString implementation" {
            val key = AspectKey.create("testKey").shouldBeRight()
            key.toString() shouldBe "testKey"
            key.value shouldBe "testKey"
        }

        "should maintain immutability" {
            val keyName = "priority"
            val key1 = AspectKey.create(keyName).shouldBeRight()
            val key2 = AspectKey.create(keyName).shouldBeRight()

            key1 shouldBe key2
            key1.value shouldBe key2.value
        }

        "should handle inline value class behavior" {
            val key = AspectKey.create("status").shouldBeRight()

            // Should be able to use in collections
            val set = setOf(key)
            set.contains(key) shouldBe true

            // Should work as map key
            val map = mapOf(key to "active")
            map[key] shouldBe "active"
        }

        "should handle property-based testing for valid keys" {
            checkAll(Arb.string(1..50)) { str ->
                val result = AspectKey.create(str)

                // Check if string matches the valid pattern
                val validPattern = Regex("^[a-zA-Z][a-zA-Z0-9_-]*$")

                if (str.isNotBlank() && str.matches(validPattern)) {
                    val key = result.shouldBeRight()
                    key.value shouldBe str
                } else {
                    result.shouldBeLeft()
                }
            }
        }

        "should create multiple unique keys" {
            val keys = listOf(
                "priority",
                "status",
                "type",
                "category",
                "assignee",
            )

            val createdKeys = mutableSetOf<String>()
            keys.forEach { keyName ->
                val result = AspectKey.create(keyName)
                val key = result.shouldBeRight()
                createdKeys.add(key.value) shouldBe true
            }

            createdKeys.size shouldBe keys.size
        }

        "should handle camelCase conventions" {
            val camelCaseKeys = listOf(
                "createdAt",
                "updatedAt",
                "dueDate",
                "startTime",
                "endTime",
                "isActive",
                "hasAttachments",
                "canEdit",
                "shouldNotify",
            )

            camelCaseKeys.forEach { key ->
                val result = AspectKey.create(key)
                val aspectKey = result.shouldBeRight()
                aspectKey.value shouldBe key
            }
        }

        "should handle snake_case conventions" {
            val snakeCaseKeys = listOf(
                "created_at",
                "updated_at",
                "due_date",
                "start_time",
                "end_time",
                "is_active",
                "has_attachments",
                "can_edit",
                "should_notify",
            )

            snakeCaseKeys.forEach { key ->
                val result = AspectKey.create(key)
                val aspectKey = result.shouldBeRight()
                aspectKey.value shouldBe key
            }
        }

        "should handle kebab-case conventions" {
            val kebabCaseKeys = listOf(
                "created-at",
                "updated-at",
                "due-date",
                "start-time",
                "end-time",
                "is-active",
                "has-attachments",
                "can-edit",
                "should-notify",
            )

            kebabCaseKeys.forEach { key ->
                val result = AspectKey.create(key)
                val aspectKey = result.shouldBeRight()
                aspectKey.value shouldBe key
            }
        }
    })
