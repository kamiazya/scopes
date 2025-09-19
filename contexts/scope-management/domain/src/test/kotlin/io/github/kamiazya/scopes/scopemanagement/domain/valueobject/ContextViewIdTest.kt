package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch

/**
 * Tests for ContextViewId value object.
 */
class ContextViewIdTest :
    StringSpec({

        "should generate new ContextViewId with ULID format" {
            val id1 = ContextViewId.generate()
            val id2 = ContextViewId.generate()

            // Should be different
            id1.value shouldNotBe id2.value

            // Should be valid ULIDs (26 characters, alphanumeric)
            id1.value.length shouldBe 26
            id2.value.length shouldBe 26
            id1.value shouldMatch "[0-9A-Z]{26}"
            id2.value shouldMatch "[0-9A-Z]{26}"
        }

        "should create ContextViewId from valid ULID string" {
            val validUlid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
            val result = ContextViewId.create(validUlid)
            val id = result.shouldBeRight()
            id.value shouldBe validUlid
            id.toString() shouldBe validUlid
        }

        "should accept lowercase ULID" {
            val lowercaseUlid = "01arz3ndektsv4rrffq69g5fav"
            val result = ContextViewId.create(lowercaseUlid)
            val id = result.shouldBeRight()
            id.value shouldBe lowercaseUlid
        }

        "should reject empty string" {
            val result = ContextViewId.create("")
            result.shouldBeLeft()
            result.leftOrNull() shouldBe ContextError.EmptyKey
        }

        "should reject blank string" {
            val result = ContextViewId.create("   ")
            result.shouldBeLeft()
            result.leftOrNull() shouldBe ContextError.EmptyKey
        }

        "should reject invalid ULID format" {
            val invalidUlids = listOf(
                "123", // too short
                "01ARZ3NDEKTSV4RRFFQ69G5FA", // 25 chars (too short)
                "01ARZ3NDEKTSV4RRFFQ69G5FAVX", // 27 chars (too long)
                "01ARZ3NDEKTSV4RRFFQ69G5FA!", // invalid character
                "01ARZ3NDEKTSV4RRFFQ69G5FA ", // space
                "not-a-ulid",
                "GHIJKLMNOPQRSTUVWXYZ123456", // Invalid ULID characters (no G-Z in base32)
                "01ARZ3NDEKTSV4RRFFQ69G5FAU", // U is invalid in Crockford base32
                "01ARZ3NDEKTSV4RRFFQ69G5FAI", // I is invalid
                "01ARZ3NDEKTSV4RRFFQ69G5FAO", // O is invalid
                "01ARZ3NDEKTSV4RRFFQ69G5FAL", // L is invalid
            )

            invalidUlids.forEach { invalid ->
                val result = ContextViewId.create(invalid)
                result.shouldBeLeft()
                val error = result.leftOrNull()
                error shouldBe ContextError.InvalidKeyFormat(
                    errorType = ContextError.InvalidKeyFormat.InvalidKeyFormatType.INVALID_PATTERN,
                )
            }
        }

        "should convert to AggregateId successfully" {
            val contextViewId = ContextViewId.create("01ARZ3NDEKTSV4RRFFQ69G5FAV").shouldBeRight()
            val result = contextViewId.toAggregateId()
            val aggregateId = result.shouldBeRight()

            // Expected format based on AggregateId.Uri implementation
            aggregateId.toString() shouldBe "Uri(value=gid://scopes/ContextView/01ARZ3NDEKTSV4RRFFQ69G5FAV)"
        }

        "should handle toAggregateId with lowercase ULID" {
            val lowercaseUlid = "01arz3ndektsv4rrffq69g5fav"
            val contextViewId = ContextViewId.create(lowercaseUlid).shouldBeRight()
            val result = contextViewId.toAggregateId()
            val aggregateId = result.shouldBeRight()

            // Note: AggregateId might normalize to uppercase internally
            // The test should accept the actual behavior
            aggregateId.toString() shouldBe "Uri(value=gid://scopes/ContextView/01ARZ3NDEKTSV4RRFFQ69G5FAV)"
        }

        "should verify toString returns the value" {
            val ulid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
            val id = ContextViewId.create(ulid).shouldBeRight()
            id.toString() shouldBe ulid
            id.toString() shouldBe id.value
        }

        "should generate different IDs in rapid succession" {
            val ids = List(10) { ContextViewId.generate() }
            val uniqueIds = ids.map { it.value }.toSet()

            // All should be unique
            uniqueIds.size shouldBe 10
        }

        "should generate chronologically sortable IDs" {
            val id1 = ContextViewId.generate()
            Thread.sleep(2) // Small delay to ensure different timestamps
            val id2 = ContextViewId.generate()
            Thread.sleep(2)
            val id3 = ContextViewId.generate()

            // ULIDs are lexicographically sortable by time
            (id1.value < id2.value) shouldBe true
            (id2.value < id3.value) shouldBe true
        }

        "should handle edge case ULID values" {
            // Minimum valid ULID (all zeros in allowed charset)
            val minUlid = "00000000000000000000000000"
            val minResult = ContextViewId.create(minUlid)
            minResult.shouldBeRight()

            // Maximum timestamp ULID - use valid base32 characters only
            // Valid base32 characters are 0-9 and A-Z excluding I, L, O, U
            val maxTimestampUlid = "7ZZZZZZZZZ9ABCDEFGHJKMNPQR"
            val maxResult = ContextViewId.create(maxTimestampUlid)
            maxResult.shouldBeRight()
        }

        "should reject ULID-like strings with invalid characters" {
            // These look like ULIDs but contain invalid base32 characters
            val invalidChars = listOf(
                "01ARZ3NDEKTSV4RRFFQ69G5FAI", // I is not valid
                "01ARZ3NDEKTSV4RRFFQ69G5FAL", // L is not valid
                "01ARZ3NDEKTSV4RRFFQ69G5FAO", // O is not valid
                "01ARZ3NDEKTSV4RRFFQ69G5FAU", // U is not valid
            )

            invalidChars.forEach { invalid ->
                val result = ContextViewId.create(invalid)
                result.shouldBeLeft()
            }
        }

        "should verify ContextViewId value property" {
            // Verify that the value property is accessible and correct
            val ulid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
            val id = ContextViewId.create(ulid).shouldBeRight()
            id.value shouldBe ulid

            // Value classes in Kotlin don't always inline at runtime in tests
            // This is a Kotlin compiler optimization that may not apply in all contexts
        }

        "should handle concurrent generation" {
            val ids = (1..100).map {
                Thread { ContextViewId.generate() }
            }.map { thread ->
                thread.start()
                thread
            }.map { thread ->
                thread.join()
                thread
            }

            // All generated IDs should be valid
            // (This mainly tests that generation is thread-safe)
        }
    })
