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
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.checkAll

/**
 * Tests for ScopeId value object.
 *
 * Business rules:
 * - Must be a valid ULID format
 * - Cannot be blank or empty
 * - Should be type-safe and immutable
 * - Can be generated or created from string
 * - Can convert to AggregateId
 */
class ScopeIdTest :
    StringSpec({

        "should generate valid ScopeId" {
            val scopeId = ScopeId.generate()
            scopeId.value shouldHaveLength 26
            scopeId.toString() shouldHaveLength 26

            // Generated ULID should match ULID pattern (26 chars, Crockford Base32)
            scopeId.value shouldMatch Regex("^[0-7][0-9A-HJKMNP-TV-Z]{25}$")
        }

        "should create multiple unique generated IDs" {
            val id1 = ScopeId.generate()
            val id2 = ScopeId.generate()
            val id3 = ScopeId.generate()

            // All should be different
            id1 shouldNotBe id2
            id2 shouldNotBe id3
            id1 shouldNotBe id3

            // All should be valid ULIDs
            id1.value shouldHaveLength 26
            id2.value shouldHaveLength 26
            id3.value shouldHaveLength 26
        }

        "should create valid ScopeId from valid ULID string" {
            val validUlid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
            val result = ScopeId.create(validUlid)
            val scopeId = result.shouldBeRight()
            scopeId.value shouldBe validUlid
            scopeId.toString() shouldBe validUlid
        }

        "should create ScopeId from edge case valid ULIDs" {
            val edgeCases = listOf(
                "00000000000000000000000000", // Minimum ULID
                "7ZZZZZZZZZZZZZZZZZZZZZZZZZ", // Maximum ULID
                "01FXJQG2C6M4VPQR5XRGMHPQR8", // Typical ULID
                "01F7MGYZXGPS2S6G3CPQHM7FSV", // Another valid ULID
            )

            edgeCases.forEach { ulid ->
                val result = ScopeId.create(ulid)
                val scopeId = result.shouldBeRight()
                scopeId.value shouldBe ulid
            }
        }

        "should reject empty string" {
            val result = ScopeId.create("")
            result.shouldBeLeft()
            result.leftOrNull() shouldBe ScopeInputError.IdError.EmptyId
        }

        "should reject blank string with spaces" {
            val result = ScopeId.create("   ")
            result.shouldBeLeft()
            result.leftOrNull() shouldBe ScopeInputError.IdError.EmptyId
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
                val result = ScopeId.create(invalidUlid)
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
                val result = ScopeId.create(invalidUlid)
                result.shouldBeLeft()
                val error = result.leftOrNull() as? ScopeInputError.IdError.InvalidIdFormat
                error?.id shouldBe invalidUlid
                error?.expectedFormat shouldBe ScopeInputError.IdError.InvalidIdFormat.IdFormatType.ULID
            }
        }

        "should reject strings starting with invalid timestamp" {
            // ULID timestamp part should be 0-7 for first character
            val invalidTimestamps = listOf(
                "81ARZ3NDEKTSV4RRFFQ69G5FAV", // Starts with 8
                "91ARZ3NDEKTSV4RRFFQ69G5FAV", // Starts with 9
                "A1ARZ3NDEKTSV4RRFFQ69G5FAV", // Starts with A
                "Z1ARZ3NDEKTSV4RRFFQ69G5FAV", // Starts with Z
            )

            invalidTimestamps.forEach { invalidUlid ->
                val result = ScopeId.create(invalidUlid)
                result.shouldBeLeft()
                val error = result.leftOrNull() as? ScopeInputError.IdError.InvalidIdFormat
                error?.id shouldBe invalidUlid
                error?.expectedFormat shouldBe ScopeInputError.IdError.InvalidIdFormat.IdFormatType.ULID
            }
        }

        "should convert to AggregateId successfully" {
            val scopeId = ScopeId.generate()
            val aggregateIdResult = scopeId.toAggregateId()

            val aggregateId = aggregateIdResult.shouldBeRight()
            aggregateId.value shouldBe "gid://scopes/Scope/${scopeId.value}"
        }

        "should convert to AggregateId for known valid IDs" {
            val validUlid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
            val scopeId = ScopeId.create(validUlid).getOrNull()!!
            val aggregateIdResult = scopeId.toAggregateId()

            val aggregateId = aggregateIdResult.shouldBeRight()
            aggregateId.value shouldBe "gid://scopes/Scope/01ARZ3NDEKTSV4RRFFQ69G5FAV"
        }

        "should maintain immutability" {
            val ulid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
            val scopeId = ScopeId.create(ulid).getOrNull()!!

            // Value should be immutable
            scopeId.value shouldBe ulid

            // toString should return the same value
            scopeId.toString() shouldBe ulid

            // Creating another with same value should be equal
            val scopeId2 = ScopeId.create(ulid).getOrNull()!!
            scopeId shouldBe scopeId2
            scopeId.value shouldBe scopeId2.value
        }

        "should handle inline value class behavior" {
            val ulid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
            val scopeId = ScopeId.create(ulid).getOrNull()!!

            // Inline value class should unwrap to its value in most contexts
            scopeId.value shouldBe ulid

            // But maintain type safety
            val anotherScopeId = ScopeId.create(ulid).getOrNull()!!
            scopeId shouldBe anotherScopeId
        }

        "should verify toString implementation" {
            val ulid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
            val scopeId = ScopeId.create(ulid).getOrNull()!!

            scopeId.toString() shouldBe ulid
            scopeId.toString() shouldBe scopeId.value
        }

        // Property-based testing
        "should always reject invalid ULID formats" {
            checkAll(
                Arb.string(1..50),
            ) { str: String ->
                // Generate strings that are definitely not valid ULIDs
                val isInvalidUlid = str.length != 26 || str.any { it.lowercaseChar() in "ilou" } || str.any { !it.isLetterOrDigit() }

                if (isInvalidUlid && str.isNotBlank()) {
                    val result = ScopeId.create(str)
                    result.shouldBeLeft()
                }
            }
        }

        "should always accept valid ULID pattern strings" {
            checkAll(
                Arb.stringPattern("[0-7][0-9A-HJKMNP-TV-Z]{25}"),
            ) { validUlidPattern ->
                val result = ScopeId.create(validUlidPattern)
                val scopeId = result.shouldBeRight()
                scopeId.value shouldBe validUlidPattern
                scopeId.toString() shouldBe validUlidPattern
            }
        }

        "should handle generated IDs in realistic scenarios" {
            // Simulate generating many IDs like in a real application
            val generatedIds = mutableSetOf<String>()

            repeat(100) {
                val scopeId = ScopeId.generate()

                // Each ID should be unique
                generatedIds.add(scopeId.value) shouldBe true

                // Each ID should be valid
                scopeId.value shouldHaveLength 26
                scopeId.value shouldMatch Regex("^[0-7][0-9A-HJKMNP-TV-Z]{25}$")

                // Each ID should convert to AggregateId
                val aggregateId = scopeId.toAggregateId().shouldBeRight()
                aggregateId.value shouldBe "gid://scopes/Scope/${scopeId.value}"
            }

            // All IDs should be unique
            generatedIds.size shouldBe 100
        }

        "should handle edge cases in string processing" {
            val edgeCases = mapOf(
                "\t01ARZ3NDEKTSV4RRFFQ69G5FAV\t" to false, // Tabs (not trimmed)
                "\n01ARZ3NDEKTSV4RRFFQ69G5FAV\n" to false, // Newlines
                " 01ARZ3NDEKTSV4RRFFQ69G5FAV " to false, // Spaces (not trimmed)
                "01ARZ3NDEKTSV4RRFFQ69G5FAV " to false, // Trailing space
                " 01ARZ3NDEKTSV4RRFFQ69G5FAV" to false, // Leading space
                "01ARZ3-DEKTSV4RRFFQ69G5FAV" to false, // Contains hyphen
                "01ARZ3_DEKTSV4RRFFQ69G5FAV" to false, // Contains underscore
            )

            edgeCases.forEach { (input, shouldBeValid) ->
                val result = ScopeId.create(input)
                if (shouldBeValid) {
                    result.shouldBeRight()
                } else {
                    result.shouldBeLeft()
                }
            }
        }
    })
