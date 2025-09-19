package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Tests for AliasName value object.
 *
 * Business rules:
 * - Must not be blank
 * - Must be between 2 and 64 characters
 * - Must contain only alphanumeric characters, hyphens, and underscores
 * - Must start with a letter
 * - Must not contain consecutive hyphens or underscores
 * - Automatically normalizes to lowercase
 */
class AliasNameTest :
    StringSpec({

        "should create valid alias name with letters only" {
            val result = AliasName.create("myalias")
            val aliasName = result.shouldBeRight()
            aliasName.value shouldBe "myalias"
            aliasName.toString() shouldBe "myalias"
        }

        "should create valid alias name with numbers" {
            val result = AliasName.create("alias123")
            val aliasName = result.shouldBeRight()
            aliasName.value shouldBe "alias123"
        }

        "should create valid alias name with hyphens" {
            val result = AliasName.create("my-alias-name")
            val aliasName = result.shouldBeRight()
            aliasName.value shouldBe "my-alias-name"
        }

        "should create valid alias name with underscores" {
            val result = AliasName.create("my_alias_name")
            val aliasName = result.shouldBeRight()
            aliasName.value shouldBe "my_alias_name"
        }

        "should create valid alias name with mixed special chars" {
            val result = AliasName.create("my-alias_name-123")
            val aliasName = result.shouldBeRight()
            aliasName.value shouldBe "my-alias_name-123"
        }

        "should normalize to lowercase" {
            val variations = listOf(
                "MyAlias",
                "MYALIAS",
                "MyALIAS",
                "myalias",
            )

            variations.forEach { input ->
                val result = AliasName.create(input)
                val aliasName = result.shouldBeRight()
                aliasName.value shouldBe "myalias"
            }
        }

        "should trim whitespace before validation" {
            val result = AliasName.create("  myalias  ")
            val aliasName = result.shouldBeRight()
            aliasName.value shouldBe "myalias"
        }

        "should reject empty string" {
            val result = AliasName.create("")
            result.shouldBeLeft()
            val error = result.leftOrNull()
            error shouldBe ScopeInputError.AliasError.EmptyAlias
        }

        "should reject blank string after trimming" {
            val result = AliasName.create("   ")
            result.shouldBeLeft()
            val error = result.leftOrNull()
            error shouldBe ScopeInputError.AliasError.EmptyAlias
        }

        "should reject alias shorter than minimum length" {
            val result = AliasName.create("a")
            result.shouldBeLeft()
            val error = result.leftOrNull() as? ScopeInputError.AliasError.AliasTooShort
            error?.minLength shouldBe 2
        }

        "should accept alias at minimum length" {
            val result = AliasName.create("ab")
            val aliasName = result.shouldBeRight()
            aliasName.value shouldBe "ab"
        }

        "should accept alias at maximum length" {
            val maxLengthAlias = "a" + "b".repeat(63) // 64 chars total
            maxLengthAlias shouldHaveLength 64

            val result = AliasName.create(maxLengthAlias)
            val aliasName = result.shouldBeRight()
            aliasName.value shouldBe maxLengthAlias
            aliasName.value shouldHaveLength 64
        }

        "should reject alias longer than maximum length" {
            val tooLongAlias = "a" + "b".repeat(64) // 65 chars total
            val result = AliasName.create(tooLongAlias)
            result.shouldBeLeft()
            val error = result.leftOrNull() as? ScopeInputError.AliasError.AliasTooLong
            error?.maxLength shouldBe 64
        }

        "should reject alias starting with number" {
            val result = AliasName.create("123alias")
            result.shouldBeLeft()
            val error = result.leftOrNull() as? ScopeInputError.AliasError.InvalidAliasFormat
            error?.alias shouldBe "123alias"
            error?.expectedPattern shouldBe ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.LOWERCASE_WITH_HYPHENS
        }

        "should reject alias starting with hyphen" {
            val result = AliasName.create("-myalias")
            result.shouldBeLeft()
            val error = result.leftOrNull() as? ScopeInputError.AliasError.InvalidAliasFormat
            error?.alias shouldBe "-myalias"
        }

        "should reject alias starting with underscore" {
            val result = AliasName.create("_myalias")
            result.shouldBeLeft()
            val error = result.leftOrNull() as? ScopeInputError.AliasError.InvalidAliasFormat
            error?.alias shouldBe "_myalias"
        }

        "should reject alias with special characters" {
            val invalidChars = listOf(
                "my@alias",
                "my#alias",
                "my\$alias",
                "my%alias",
                "my&alias",
                "my*alias",
                "my+alias",
                "my=alias",
                "my!alias",
                "my alias", // space
                "my.alias",
                "my,alias",
                "my:alias",
                "my;alias",
                "my'alias",
                "my\"alias",
                "my/alias",
                "my\\alias",
            )

            invalidChars.forEach { invalid ->
                val result = AliasName.create(invalid)
                result.shouldBeLeft()
                val error = result.leftOrNull() as? ScopeInputError.AliasError.InvalidAliasFormat
                error?.alias shouldBe invalid.lowercase()
            }
        }

        "should reject alias with consecutive hyphens" {
            val result = AliasName.create("my--alias")
            result.shouldBeLeft()
            val error = result.leftOrNull() as? ScopeInputError.AliasError.InvalidAliasFormat
            error?.alias shouldBe "my--alias"
        }

        "should reject alias with consecutive underscores" {
            val result = AliasName.create("my__alias")
            result.shouldBeLeft()
            val error = result.leftOrNull() as? ScopeInputError.AliasError.InvalidAliasFormat
            error?.alias shouldBe "my__alias"
        }

        "should reject alias with mixed consecutive special chars" {
            val invalidPatterns = listOf(
                "my-_alias",
                "my_-alias",
                "my---alias",
                "my___alias",
            )

            invalidPatterns.forEach { invalid ->
                val result = AliasName.create(invalid)
                result.shouldBeLeft()
            }
        }

        "should handle control characters properly" {
            val invalidStrings = listOf(
                "my\nalias", // newline
                "my\talias", // tab
                "my\ralias", // carriage return
                "my\u0000alias", // null character
            )

            invalidStrings.forEach { invalid ->
                val result = AliasName.create(invalid)
                result.shouldBeLeft()
            }
        }

        "should maintain immutability" {
            val alias = "myalias"
            val aliasName = AliasName.create(alias).getOrNull()!!

            // Value should be immutable
            aliasName.value shouldBe alias

            // toString should return the same value
            aliasName.toString() shouldBe alias

            // Creating another with same value should be equal
            val aliasName2 = AliasName.create(alias).getOrNull()!!
            aliasName shouldBe aliasName2
            aliasName.value shouldBe aliasName2.value
        }

        "should handle inline value class behavior" {
            val alias = "myalias"
            val aliasName = AliasName.create(alias).getOrNull()!!

            // Inline value class should unwrap to its value in most contexts
            aliasName.value shouldBe alias

            // But maintain type safety
            val anotherAliasName = AliasName.create(alias).getOrNull()!!
            aliasName shouldBe anotherAliasName
        }

        "should handle property-based testing for valid aliases" {
            checkAll(Arb.string(2..64)) { str ->
                val normalized = str.trim().lowercase()
                val result = AliasName.create(str)

                // Check if the string matches valid alias pattern
                // Updated regex to match the implementation: must not end with hyphen or underscore
                val validPattern = Regex("^[a-z][a-z0-9]$|^[a-z][a-z0-9-_]*[a-z0-9]$")
                val hasConsecutiveSpecialChars = normalized.contains(Regex("[-_]{2,}"))

                if (normalized.matches(validPattern) && !hasConsecutiveSpecialChars) {
                    result.shouldBeRight()
                    result.getOrNull()?.value shouldBe normalized
                } else if (normalized.isNotBlank() && normalized.length >= 2 && normalized.length <= 64) {
                    // If it doesn't match pattern, should be invalid format
                    result.shouldBeLeft()
                    val error = result.leftOrNull()
                    when (error) {
                        is ScopeInputError.AliasError.InvalidAliasFormat -> {
                            error.alias shouldBe normalized
                        }
                        else -> {} // Other errors are also valid
                    }
                }
            }
        }

        "should verify toString implementation" {
            val alias = "myalias"
            val aliasName = AliasName.create(alias).getOrNull()!!

            aliasName.toString() shouldBe alias
            aliasName.value shouldBe alias
        }

        "should handle realistic alias scenarios" {
            val validAliases = listOf(
                "api",
                "api-v2",
                "user_service",
                "auth2",
                "web-app",
                "backend_api",
                "feature-123",
                "test_env_01",
                "prod",
                "staging-env",
            )

            validAliases.forEach { alias ->
                val result = AliasName.create(alias)
                result.shouldBeRight()
                result.getOrNull()?.value shouldBe alias.lowercase()
            }
        }

        "should handle edge cases in string processing" {
            val edgeCases = mapOf(
                "  ab  " to true, // Min length with spaces
                "\tab\t" to true, // Tabs (should be trimmed)
                "\nab\n" to true, // Newlines (should be trimmed)
                "AB" to true, // Uppercase at min length
                "a-b" to true, // Valid with hyphen
                "a_b" to true, // Valid with underscore
                "a--b" to false, // Consecutive hyphens
                "a__b" to false, // Consecutive underscores
                "a-" to false, // Ends with hyphen (too short after trimming)
                "a_" to false, // Ends with underscore (too short after trimming)
                "-a" to false, // Starts with hyphen
                "_a" to false, // Starts with underscore
                "a b" to false, // Contains space
                "a@b" to false, // Contains invalid character
            )

            edgeCases.forEach { (input, shouldBeValid) ->
                val result = AliasName.create(input)
                if (shouldBeValid) {
                    result.shouldBeRight()
                } else {
                    result.shouldBeLeft()
                }
            }
        }

        "should create multiple unique aliases" {
            val aliases = listOf(
                "feature-auth",
                "feature-login",
                "feature-logout",
                "api-users",
                "api-products",
            )

            val createdAliases = mutableSetOf<String>()
            aliases.forEach { alias ->
                val result = AliasName.create(alias)
                val aliasName = result.shouldBeRight()
                createdAliases.add(aliasName.value) shouldBe true
            }

            createdAliases.size shouldBe aliases.size
        }
    })
