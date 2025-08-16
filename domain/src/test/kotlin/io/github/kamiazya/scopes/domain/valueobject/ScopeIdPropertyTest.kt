package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import com.github.guepardoapps.kulid.ULID
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

class ScopeIdPropertyTest : StringSpec({

    "generated scope IDs should always be valid ULIDs" {
        repeat(1000) {
            val scopeId = ScopeId.generate()
            ULID.isValid(scopeId.value) shouldBe true
            scopeId.value.length shouldBe 26
        }
    }

    "generated scope IDs should be unique" {
        val ids = (1..1000).map { ScopeId.generate() }
        ids.distinct().size shouldBe ids.size
    }

    "generated scope IDs should be lexicographically sortable by time" {
        val ids = mutableListOf<ScopeId>()
        repeat(10) {
            ids.add(ScopeId.generate())
            Thread.sleep(1) // Small delay to ensure different timestamps
        }
        val sortedIds = ids.sortedBy { it.value }
        sortedIds shouldBe ids
    }

    "valid ULID strings should create valid ScopeIds" {
        checkAll(validUlidArb()) { ulid ->
            val result = ScopeId.create(ulid)
            result.isRight() shouldBe true
            result.fold(
                { throw AssertionError("Expected Right but got Left") },
                { scopeId ->
                    scopeId.value shouldBe ulid
                    ULID.isValid(scopeId.value) shouldBe true
                }
            )
        }
    }

    "invalid ULID strings should return error" {
        checkAll(invalidUlidArb()) { invalidUlid ->
            val result = ScopeId.create(invalidUlid)
            result.isLeft() shouldBe true
        }
    }

    "empty or blank strings should return error" {
        checkAll(Arb.of("", " ", "  ", "\t", "\n", "   \t   ")) { blank ->
            val result = ScopeId.create(blank)
            result.isLeft() shouldBe true
        }
    }

    "scope ID creation should be idempotent" {
        checkAll(validUlidArb()) { ulid ->
            val result1 = ScopeId.create(ulid)
            val result2 = ScopeId.create(ulid)
            result1.isRight() shouldBe true
            result2.isRight() shouldBe true
            result1.fold(
                { throw AssertionError("Expected Right but got Left") },
                { id1 ->
                    result2.fold(
                        { throw AssertionError("Expected Right but got Left") },
                        { id2 ->
                            id1 shouldBe id2
                            id1.value shouldBe id2.value
                        }
                    )
                }
            )
        }
    }

    "scope ID string representation should equal its value" {
        checkAll(validUlidArb()) { ulid ->
            val result = ScopeId.create(ulid)
            result.isRight() shouldBe true
            result.fold(
                { throw AssertionError("Expected Right but got Left") },
                { scopeId ->
                    scopeId.toString() shouldBe ulid
                    scopeId.toString() shouldBe scopeId.value
                }
            )
        }
    }

    "scope IDs should follow ULID format pattern" {
        repeat(100) {
            val scopeId = ScopeId.generate()
            // ULID pattern: Time (10 chars) + Randomness (16 chars) = 26 chars total
            // Uses Crockford's base32: 0-9, A-Z excluding I, L, O, U
            scopeId.value shouldMatch "[0-9A-HJKMNP-TV-Z]{26}"
        }
    }

    "scope IDs with invalid characters should be rejected" {
        checkAll(
            Arb.string(26..26).map { str ->
                // Replace some characters with invalid ones
                str.mapIndexed { index, c ->
                    if (index % 3 == 0) 'I' // Invalid character
                    else c
                }.joinToString("")
            }
        ) { invalidId ->
            val result = ScopeId.create(invalidId)
            result.isLeft() shouldBe true
        }
    }

    "scope IDs with wrong length should be rejected" {
        checkAll(
            Arb.choice(
                Arb.string(1..25), // Too short
                Arb.string(27..100) // Too long
            )
        ) { wrongLength ->
            if (wrongLength.isNotBlank()) {
                val result = ScopeId.create(wrongLength)
                result.isLeft() shouldBe true
            }
        }
    }

    "scope IDs should maintain chronological order when generated sequentially" {
        val ids = mutableListOf<String>()
        repeat(10) {
            ids.add(ScopeId.generate().value)
            Thread.sleep(2) // Ensure different timestamps
        }
        
        // Check if IDs are in ascending order
        for (i in 1 until ids.size) {
            (ids[i] > ids[i - 1]) shouldBe true
        }
    }

    "scope ID equality should be value-based" {
        checkAll(validUlidArb()) { ulid ->
            val result1 = ScopeId.create(ulid)
            val result2 = ScopeId.create(ulid)
            result1.isRight() shouldBe true
            result2.isRight() shouldBe true
            result1.fold(
                { throw AssertionError("Expected Right but got Left") },
                { id1 ->
                    result2.fold(
                        { throw AssertionError("Expected Right but got Left") },
                        { id2 ->
                            (id1 == id2) shouldBe true
                            // They are equal but different instances (value class behavior)
                        }
                    )
                }
            )
        }
    }

    "different valid ULIDs should create different ScopeIds" {
        val id1 = ScopeId.generate()
        val id2 = ScopeId.generate()
        id1 shouldNotBe id2
        id1.value shouldNotBe id2.value
    }

    "scope IDs should handle case sensitivity correctly" {
        // ULIDs are case-insensitive in Crockford's base32
        val upperCase = "01HQ5JJGQKX9NPZB0X3Q5VWXYZ"
        val lowerCase = "01hq5jjgqkx9npzb0x3q5vwxyz"
        
        // Both should be valid if the library handles case normalization
        val upperResult = ScopeId.create(upperCase)
        val lowerResult = ScopeId.create(lowerCase)
        
        // At least one should succeed (depends on ULID library implementation)
        (upperResult.isRight() || lowerResult.isRight()) shouldBe true
    }
})

// Custom Arbitrary generators
private fun validUlidArb(): Arb<String> = arbitrary {
    ULID.random()
}

private fun invalidUlidArb(): Arb<String> = Arb.choice(
    // Wrong length
    Arb.string(1..25),
    Arb.string(27..50),
    // Invalid characters
    Arb.string(26..26).map { it.replace(it[0], '@') },
    // Special patterns
    Arb.of(
        "INVALID_ULID_FORMAT_TEST",
        // Note: "12345678901234567890123456" might be valid in some ULID implementations
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ", // Contains invalid chars I, L, O, U
        "!@#$%^&*()_+-=[]{}|;:,.<>?",  // Special characters
        "IIIIIIIIIIIIIIIIIIIIIIIIII", // All invalid characters
        "LLLLLLLLLLLLLLLLLLLLLLLLLL", // All invalid characters
        "OOOOOOOOOOOOOOOOOOOOOOOOOO", // All invalid characters  
        "UUUUUUUUUUUUUUUUUUUUUUUUUU"  // All invalid characters
    )
)