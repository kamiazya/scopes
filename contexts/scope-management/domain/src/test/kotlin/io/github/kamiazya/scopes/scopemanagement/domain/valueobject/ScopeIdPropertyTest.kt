package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.github.kamiazya.scopes.platform.commons.id.ULID
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

class ScopeIdPropertyTest :
    StringSpec({

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
                    },
                )
            }
        }

        "invalid strings should fail to create ScopeIds" {
            checkAll(invalidUlidArb()) { invalid ->
                val result = ScopeId.create(invalid)
                result.isLeft() shouldBe true
            }
        }

        "empty string should fail to create ScopeId" {
            val result = ScopeId.create("")
            result.isLeft() shouldBe true
        }

        "blank string should fail to create ScopeId" {
            val result = ScopeId.create("   ")
            result.isLeft() shouldBe true
        }

        "ScopeId should convert to AggregateId correctly" {
            checkAll(validUlidArb()) { ulid ->
                val scopeId = ScopeId.create(ulid).getOrNull()!!
                val aggregateId = scopeId.toAggregateId()
                aggregateId.isRight() shouldBe true
                aggregateId.fold(
                    { throw AssertionError("Expected Right but got Left") },
                    { aggId ->
                        // AggregateId.Uri has value property containing the full URI
                        aggId.value shouldBe "gid://scopes/Scope/$ulid"
                    },
                )
            }
        }

        "toString should return the underlying value" {
            checkAll(validUlidArb()) { ulid ->
                val scopeId = ScopeId.create(ulid).getOrNull()!!
                scopeId.toString() shouldBe ulid
            }
        }

        "equals and hashCode should work correctly" {
            checkAll(validUlidArb()) { ulid ->
                val scopeId1 = ScopeId.create(ulid).getOrNull()!!
                val scopeId2 = ScopeId.create(ulid).getOrNull()!!
                scopeId1 shouldBe scopeId2
                scopeId1.hashCode() shouldBe scopeId2.hashCode()
            }
        }

        "different ScopeIds should not be equal" {
            val scopeId1 = ScopeId.generate()
            val scopeId2 = ScopeId.generate()
            scopeId1 shouldNotBe scopeId2
        }
    })

// Test helpers
private fun validUlidArb(): Arb<String> = arbitrary {
    ULID.generate().toString()
}

private fun invalidUlidArb(): Arb<String> = Arb.choice(
    Arb.string(1, 25), // Too short
    Arb.string(27, 50), // Too long
    Arb.string(26, 26).map { it.replace(Regex("[0-9A-Za-z]"), "!") }, // Invalid characters
    Arb.string(26, 26).map { "!" + it.substring(1) }, // Invalid prefix
)
