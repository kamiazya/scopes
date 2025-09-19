package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldMatch
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Tests for AliasId value object.
 *
 * Business rules:
 * - Must be a valid ULID format
 * - Cannot be blank or empty
 * - Should be type-safe and immutable
 * - Can be generated or created from string
 * - Can convert to AggregateId
 * - Trims whitespace before validation
 */
class AliasIdTest :
    StringSpec({

        "should generate valid AliasId" {
            val aliasId = AliasId.generate()
            aliasId.value shouldHaveLength 26
            aliasId.toString() shouldHaveLength 26

            // Generated ULID should match ULID pattern (26 chars, Crockford Base32)
            aliasId.value shouldMatch Regex("^[0-7][0-9A-HJKMNP-TV-Z]{25}$")
        }

        "should create multiple unique generated IDs" {
            val id1 = AliasId.generate()
            val id2 = AliasId.generate()
            val id3 = AliasId.generate()

            // All should be different
            id1 shouldNotBe id2
            id2 shouldNotBe id3
            id1 shouldNotBe id3

            // All should be valid ULIDs
            id1.value shouldHaveLength 26
            id2.value shouldHaveLength 26
            id3.value shouldHaveLength 26
        }

        "should create from valid ULID string" {
            val validUlid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
            val result = AliasId.create(validUlid)

            val aliasId = result.shouldBeRight()
            aliasId.value shouldBe validUlid
            aliasId.toString() shouldBe validUlid
        }

        "should trim whitespace before validation" {
            val validUlid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
            val withSpaces = "  $validUlid  "
            val result = AliasId.create(withSpaces)

            val aliasId = result.shouldBeRight()
            aliasId.value shouldBe validUlid // Should be trimmed
        }

        "should reject empty string" {
            val result = AliasId.create("")
            result.shouldBeLeft()
            val error = result.leftOrNull()
            error shouldBe ScopeInputError.IdError.EmptyId
        }

        "should reject blank string after trimming" {
            val result = AliasId.create("   ")
            result.shouldBeLeft()
            val error = result.leftOrNull()
            error shouldBe ScopeInputError.IdError.EmptyId
        }

        "should reject strings with invalid ULID characters" {
            val invalidChars = listOf(
                "01ARZ3NDEKTSV4RRFFQ69G5FAI", // Contains 'I'
                "01ARZ3NDEKTSV4RRFFQ69G5FAL", // Contains 'L'
                "01ARZ3NDEKTSV4RRFFQ69G5FAO", // Contains 'O'
                "01ARZ3NDEKTSV4RRFFQ69G5FAU", // Contains 'U'
                "8ZZZZZZZZZ0000000000000000", // Timestamp too large (exceeds 48-bit max)
                "01ARZ3NDEKTSV4RRFFQ69G5FA!", // Contains special character
                "01ARZ3NDEKTSV4RRFFQ69G5FA@", // Contains special character
            )

            invalidChars.forEach { invalidUlid ->
                val result = AliasId.create(invalidUlid)
                result.shouldBeLeft()
                val error = result.leftOrNull() as? ScopeInputError.IdError.InvalidIdFormat
                error?.id shouldBe invalidUlid
                error?.expectedFormat shouldBe ScopeInputError.IdError.InvalidIdFormat.IdFormatType.ULID
            }
        }

        "should reject strings with invalid ULID length" {
            val invalidLengths = listOf(
                "01ARZ3NDEKTSV4RRFFQ69G5FA", // Too short (25 chars)
                "01ARZ3NDEKTSV4RRFFQ69G5FAVX", // Too long (27 chars)
                "01ARZ3", // Much too short
                "", // Empty (handled by empty check first)
            )

            invalidLengths.filter { it.isNotBlank() }.forEach { invalidUlid ->
                val result = AliasId.create(invalidUlid)
                result.shouldBeLeft()
                val error = result.leftOrNull() as? ScopeInputError.IdError.InvalidIdFormat
                error?.id shouldBe invalidUlid
                error?.expectedFormat shouldBe ScopeInputError.IdError.InvalidIdFormat.IdFormatType.ULID
            }
        }

        "should accept uppercase and lowercase ULID" {
            val uppercase = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
            val lowercase = "01arz3ndektsv4rrffq69g5fav"

            val upperResult = AliasId.create(uppercase)
            val lowerResult = AliasId.create(lowercase)

            upperResult.shouldBeRight()
            lowerResult.shouldBeRight()

            // Both should normalize to uppercase
            upperResult.getOrNull()?.value shouldBe uppercase
            lowerResult.getOrNull()?.value shouldBe uppercase
        }

        "should reject null characters and control characters" {
            val invalidStrings = listOf(
                "01ARZ3NDEKTSV4RRFFQ69G5FA\u0000", // Null character
                "01ARZ3NDEKTSV4RRFFQ\n69G5FAV", // Newline
                "01ARZ3NDEKTSV4RRFFQ\t69G5FAV", // Tab
                "01ARZ3NDEKTSV4RRFFQ\r69G5FAV", // Carriage return
            )

            invalidStrings.forEach { invalid ->
                val result = AliasId.create(invalid)
                result.shouldBeLeft()
            }
        }

        "should convert to AggregateId successfully" {
            val aliasId = AliasId.generate()
            val aggregateIdResult = aliasId.toAggregateId()

            val aggregateId = aggregateIdResult.shouldBeRight()
            aggregateId.value shouldBe "gid://scopes/Alias/${aliasId.value}"
        }

        "should convert to AggregateId for known valid IDs" {
            val validUlid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
            val aliasId = AliasId.create(validUlid).getOrNull()!!
            val aggregateIdResult = aliasId.toAggregateId()

            val aggregateId = aggregateIdResult.shouldBeRight()
            aggregateId.value shouldBe "gid://scopes/Alias/01ARZ3NDEKTSV4RRFFQ69G5FAV"
        }

        "should maintain immutability" {
            val ulid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
            val aliasId = AliasId.create(ulid).getOrNull()!!

            // Value should be immutable
            aliasId.value shouldBe ulid

            // toString should return the same value
            aliasId.toString() shouldBe ulid

            // Creating another with same value should be equal
            val aliasId2 = AliasId.create(ulid).getOrNull()!!
            aliasId shouldBe aliasId2
            aliasId.value shouldBe aliasId2.value
        }

        "should handle inline value class behavior" {
            val ulid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
            val aliasId = AliasId.create(ulid).getOrNull()!!

            // Inline value class should unwrap to its value in most contexts
            aliasId.value shouldBe ulid

            // But maintain type safety
            val anotherAliasId = AliasId.create(ulid).getOrNull()!!
            aliasId shouldBe anotherAliasId
        }

        "should verify toString implementation" {
            val ulid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
            val aliasId = AliasId.create(ulid).getOrNull()!!

            aliasId.toString() shouldBe ulid
            aliasId.value shouldBe ulid
        }

        "should handle property-based testing for valid ULID creation" {
            // Test with strings that could contain invalid ULID characters
            checkAll(Arb.string(26..26)) { str ->
                val result = AliasId.create(str)

                // Check if the string matches valid ULID pattern
                if (str.uppercase().matches(Regex("^[0-7][0-9A-HJKMNP-TV-Z]{25}$"))) {
                    result.shouldBeRight()
                    result.getOrNull()?.value shouldBe str.uppercase() // ULID normalizes to uppercase
                } else if (str.trim().length == 26) {
                    // If it's exactly 26 chars but doesn't match ULID pattern, should be invalid
                    result.shouldBeLeft()
                    val error = result.leftOrNull() as? ScopeInputError.IdError.InvalidIdFormat
                    error?.expectedFormat shouldBe ScopeInputError.IdError.InvalidIdFormat.IdFormatType.ULID
                }
            }
        }

        "should handle generated IDs in realistic scenarios" {
            // Simulate generating many IDs like in a real application
            val generatedIds = mutableSetOf<String>()

            repeat(100) {
                val aliasId = AliasId.generate()

                // Each ID should be unique
                generatedIds.add(aliasId.value) shouldBe true

                // Each ID should be valid
                aliasId.value shouldHaveLength 26
                aliasId.value shouldMatch Regex("^[0-7][0-9A-HJKMNP-TV-Z]{25}$")

                // Each ID should convert to AggregateId
                val aggregateId = aliasId.toAggregateId().shouldBeRight()
                aggregateId.value shouldBe "gid://scopes/Alias/${aliasId.value}"
            }

            // All IDs should be unique
            generatedIds.size shouldBe 100
        }

        "should handle edge cases in string processing" {
            val edgeCases = mapOf(
                "\t01ARZ3NDEKTSV4RRFFQ69G5FAV\t" to true, // Tabs (should be trimmed)
                "\n01ARZ3NDEKTSV4RRFFQ69G5FAV\n" to true, // Newlines (should be trimmed)
                " 01ARZ3NDEKTSV4RRFFQ69G5FAV " to true, // Spaces (should be trimmed)
                "01ARZ3NDEKTSV4RRFFQ69G5FAV " to true, // Trailing space (should be trimmed)
                " 01ARZ3NDEKTSV4RRFFQ69G5FAV" to true, // Leading space (should be trimmed)
                "01ARZ3-DEKTSV4RRFFQ69G5FAV" to false, // Contains hyphen
                "01ARZ3_DEKTSV4RRFFQ69G5FAV" to false, // Contains underscore
            )

            edgeCases.forEach { (input, shouldBeValid) ->
                val result = AliasId.create(input)
                if (shouldBeValid) {
                    result.shouldBeRight()
                } else {
                    result.shouldBeLeft()
                }
            }
        }
    })
