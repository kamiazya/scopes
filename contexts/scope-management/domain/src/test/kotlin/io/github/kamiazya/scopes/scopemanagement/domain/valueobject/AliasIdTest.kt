package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.github.kamiazya.scopes.platform.commons.id.ULID
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

class AliasIdTest :
    DescribeSpec({
        describe("AliasId") {
            describe("generate") {
                it("should generate unique AliasIds") {
                    val id1 = AliasId.generate()
                    val id2 = AliasId.generate()

                    id1 shouldNotBe id2
                    id1.value shouldNotBe id2.value
                }

                it("should generate valid ULID format") {
                    val aliasId = AliasId.generate()

                    ULID.isValid(aliasId.value) shouldBe true
                }

                it("should generate IDs with correct length") {
                    val aliasId = AliasId.generate()
                    aliasId.value.length shouldBe 26 // ULID string length is always 26
                }

                it("should generate chronologically sortable IDs") {
                    val id1 = AliasId.generate()
                    Thread.sleep(1) // Ensure different timestamp
                    val id2 = AliasId.generate()

                    (id1.value < id2.value) shouldBe true
                }
            }

            describe("create") {
                it("should create AliasId from valid ULID string") {
                    val ulidString = ULID.generate().toString()
                    val result = AliasId.create(ulidString)

                    result.shouldBeRight()
                    result.getOrNull()?.value shouldBe ulidString
                }

                it("should return error for invalid ULID string") {
                    // Test clearly invalid cases
                    AliasId.create("").shouldBeLeft()
                    AliasId.create("  ").shouldBeLeft()
                    AliasId.create("invalid").shouldBeLeft()
                    AliasId.create("123").shouldBeLeft()

                    // Test strings with invalid characters
                    AliasId.create("!@#$%^&*()_+-=[]{}|;':\",./<>?").shouldBeLeft()
                    // Note: The ULID library may have different validation rules than expected
                }

                it("should accept all valid ULID strings") {
                    repeat(100) {
                        val validUlid = ULID.generate().toString()
                        val result = AliasId.create(validUlid)
                        result.shouldBeRight()
                    }
                }

                it("should trim input and handle valid result") {
                    val validUlid = ULID.generate().toString()
                    val result = AliasId.create("  $validUlid  ")

                    result.shouldBeRight()
                    result.getOrNull()?.value shouldBe validUlid
                }
            }

            describe("toString") {
                it("should return the ULID string value") {
                    val ulidString = ULID.generate().toString()
                    val aliasId = AliasId.create(ulidString).getOrNull()!!

                    aliasId.toString() shouldBe ulidString
                }
            }

            describe("toAggregateId") {
                it("should convert to AggregateId with correct URI format") {
                    val aliasId = AliasId.generate()
                    val result = aliasId.toAggregateId()

                    result.shouldBeRight()
                    // The implementation creates URI format through AggregateId.Uri.create
                    // Check that the result is valid AggregateId containing the alias ID
                    result.getOrNull()?.let { aggregateId ->
                        aggregateId.value.contains(aliasId.value) shouldBe true
                    }
                }
            }

            describe("equals and hashCode") {
                it("should be equal for same ULID values") {
                    val ulidString = ULID.generate().toString()
                    val id1 = AliasId.create(ulidString).getOrNull()!!
                    val id2 = AliasId.create(ulidString).getOrNull()!!

                    (id1 == id2) shouldBe true
                    id1.hashCode() shouldBe id2.hashCode()
                }

                it("should not be equal for different ULID values") {
                    val id1 = AliasId.generate()
                    val id2 = AliasId.generate()

                    (id1 == id2) shouldBe false
                }
            }

            describe("property-based tests") {
                it("should maintain value consistency for valid ULIDs") {
                    checkAll(validUlidArb) { str ->
                        val result = AliasId.create(str)
                        result.shouldBeRight()
                        result.getOrNull()?.let { aliasId ->
                            aliasId.value shouldBe str
                            aliasId.toString() shouldBe str
                        }
                    }
                }

                it("should generate unique IDs in parallel") {
                    val ids = mutableSetOf<String>()
                    val threads = (1..10).map {
                        Thread {
                            repeat(100) {
                                synchronized(ids) {
                                    ids.add(AliasId.generate().value)
                                }
                            }
                        }
                    }

                    threads.forEach { it.start() }
                    threads.forEach { it.join() }

                    ids.size shouldBe 1000 // All IDs should be unique
                }
            }
        }
    })

// Property-based testing generators
private val validUlidArb = Arb.string(26, 26).filter { str ->
    ULID.isValid(str)
}
