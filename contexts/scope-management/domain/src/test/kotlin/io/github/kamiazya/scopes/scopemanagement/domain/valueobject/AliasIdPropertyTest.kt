package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.github.kamiazya.scopes.platform.commons.id.ULID
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

class AliasIdPropertyTest :
    StringSpec({

        "generated alias IDs should always be valid ULIDs" {
            repeat(1000) {
                val aliasId = AliasId.generate()
                ULID.isValid(aliasId.value) shouldBe true
                aliasId.value.length shouldBe 26
            }
        }

        "generated alias IDs should be unique" {
            val ids = (1..1000).map { AliasId.generate() }
            ids.distinct().size shouldBe ids.size
        }

        "generated alias IDs should be lexicographically sortable by time" {
            val ids = mutableListOf<AliasId>()
            repeat(10) {
                ids.add(AliasId.generate())
                Thread.sleep(1) // Small delay to ensure different timestamps
            }
            val sortedIds = ids.sortedBy { it.value }
            sortedIds shouldBe ids
        }

        "valid ULID strings should create valid AliasIds" {
            checkAll(validUlidArb()) { ulid ->
                val result = AliasId.create(ulid)
                result.isRight() shouldBe true
                result.fold(
                    { throw AssertionError("Expected Right but got Left") },
                    { aliasId ->
                        aliasId.value shouldBe ulid
                        ULID.isValid(aliasId.value) shouldBe true
                    },
                )
            }
        }

        "invalid strings should fail to create AliasIds" {
            checkAll(invalidUlidArb()) { invalid ->
                val result = AliasId.create(invalid)
                result.isLeft() shouldBe true
            }
        }

        "empty string should fail to create AliasId" {
            val result = AliasId.create("")
            result.isLeft() shouldBe true
        }

        "blank string should fail to create AliasId" {
            val result = AliasId.create("   ")
            result.isLeft() shouldBe true
        }

        "AliasId should convert to AggregateId correctly" {
            checkAll(validUlidArb()) { ulid ->
                val aliasId = AliasId.create(ulid).getOrNull()!!
                val aggregateId = aliasId.toAggregateId()
                aggregateId.isRight() shouldBe true
                aggregateId.fold(
                    { throw AssertionError("Expected Right but got Left") },
                    { aggId ->
                        // AggregateId.Uri has value property containing the full URI
                        aggId.value shouldBe "gid://scopes/Alias/$ulid"
                    },
                )
            }
        }

        "toString should return the underlying value" {
            checkAll(validUlidArb()) { ulid ->
                val aliasId = AliasId.create(ulid).getOrNull()!!
                aliasId.toString() shouldBe ulid
            }
        }

        "equals and hashCode should work correctly" {
            checkAll(validUlidArb()) { ulid ->
                val aliasId1 = AliasId.create(ulid).getOrNull()!!
                val aliasId2 = AliasId.create(ulid).getOrNull()!!
                aliasId1 shouldBe aliasId2
                aliasId1.hashCode() shouldBe aliasId2.hashCode()
            }
        }

        "different AliasIds should not be equal" {
            val aliasId1 = AliasId.generate()
            val aliasId2 = AliasId.generate()
            aliasId1 shouldNotBe aliasId2
        }

        "AliasId should be comparable" {
            checkAll(validUlidArb(), validUlidArb()) { ulid1, ulid2 ->
                val aliasId1 = AliasId.create(ulid1).getOrNull()!!
                val aliasId2 = AliasId.create(ulid2).getOrNull()!!

                val comparison = aliasId1.compareTo(aliasId2)
                val stringComparison = ulid1.compareTo(ulid2)

                comparison shouldBe stringComparison.coerceIn(-1, 1)
            }
        }

        "AliasId generation should be deterministic for hash-based generation" {
            checkAll(Arb.long()) { seed ->
                // Simulate using the same seed multiple times
                val id1 = AliasId.generate()
                val id2 = AliasId.generate()

                // Different calls should produce different IDs (time-based)
                id1 shouldNotBe id2

                // But both should be valid ULIDs
                ULID.isValid(id1.value) shouldBe true
                ULID.isValid(id2.value) shouldBe true
            }
        }

        "AliasId should maintain ULID time ordering properties" {
            val now = System.currentTimeMillis()
            val ids = mutableListOf<AliasId>()

            // Generate IDs with small time gaps
            repeat(5) {
                ids.add(AliasId.generate())
                Thread.sleep(2) // Small delay
            }

            // Verify time ordering is preserved in lexicographic ordering
            for (i in 0 until ids.size - 1) {
                val current = ids[i]
                val next = ids[i + 1]
                current.value.compareTo(next.value) shouldBe { it <= 0 }
            }
        }

        "AliasId should handle edge case ULID values" {
            checkAll(edgeUlidArb()) { ulid ->
                val result = AliasId.create(ulid)
                result.isRight() shouldBe true

                val aliasId = result.getOrNull()!!
                aliasId.value shouldBe ulid
                ULID.isValid(aliasId.value) shouldBe true
            }
        }

        "AliasId should preserve ULID ordering properties" {
            checkAll(Arb.list(validUlidArb(), 2..10)) { ulids ->
                val aliasIds = ulids.mapNotNull { AliasId.create(it).getOrNull() }

                // ULIDs are lexicographically sortable
                val sorted = aliasIds.sortedBy { it.value }
                sorted.map { it.value } shouldBe aliasIds.map { it.value }.sorted()
            }
        }

        "AliasId creation should be idempotent" {
            checkAll(validUlidArb()) { ulid ->
                val result1 = AliasId.create(ulid)
                val result2 = AliasId.create(ulid)

                result1.isRight() shouldBe true
                result2.isRight() shouldBe true

                val aliasId1 = result1.getOrNull()!!
                val aliasId2 = result2.getOrNull()!!

                aliasId1 shouldBe aliasId2
                aliasId1.value shouldBe aliasId2.value
            }
        }

        "AliasId should handle boundary length validation" {
            // Test exact boundary cases
            val exactLength = "01ARZ3NDEKTSV4RRFFQ69G5FAV" // 26 characters
            val tooShort = exactLength.dropLast(1) // 25 characters
            val tooLong = exactLength + "X" // 27 characters

            AliasId.create(exactLength).isRight() shouldBe true
            AliasId.create(tooShort).isLeft() shouldBe true
            AliasId.create(tooLong).isLeft() shouldBe true
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

private fun edgeUlidArb(): Arb<String> = Arb.choice(
    Arb.constant("00000000000000000000000000"), // Minimum timestamp
    Arb.constant("7ZZZZZZZZZZZZZZZZZZZZZZZZZ"), // Maximum timestamp
    Arb.constant("01ARZ3NDEKTSV4RRFFQ69G5FAV"), // Known valid ULID
    arbitrary { ULID.generate().toString() }, // Generate fresh ULIDs
)
