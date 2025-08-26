package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

class AliasNameTest :
    DescribeSpec({
        describe("AliasName") {
            describe("create") {
                it("should create valid alias name with alphanumeric characters") {
                    val result = AliasName.create("validAlias123")
                    result.shouldBeRight()
                    result.getOrNull()?.value shouldBe "validAlias123"
                }

                it("should create valid alias name with hyphens and underscores") {
                    val result = AliasName.create("valid-alias_name")
                    result.shouldBeRight()
                    result.getOrNull()?.value shouldBe "valid-alias_name"
                }

                it("should create valid alias name with minimum length (2 chars)") {
                    val result = AliasName.create("ab")
                    result.shouldBeRight()
                    result.getOrNull()?.value shouldBe "ab"
                }

                it("should create valid alias name with maximum length (64 chars)") {
                    val longName = "a".repeat(64)
                    val result = AliasName.create(longName)
                    result.shouldBeRight()
                    result.getOrNull()?.value shouldBe longName
                }

                it("should fail with empty string") {
                    val result = AliasName.create("")
                    result.shouldBeLeft()
                    result.leftOrNull()?.shouldBeInstanceOf<ScopeInputError.AliasError.Empty>()
                }

                it("should fail with blank string") {
                    val result = AliasName.create("   ")
                    result.shouldBeLeft()
                    result.leftOrNull()?.shouldBeInstanceOf<ScopeInputError.AliasError.Empty>()
                }

                it("should fail with single character") {
                    val result = AliasName.create("a")
                    result.shouldBeLeft()
                    result.leftOrNull()?.shouldBeInstanceOf<ScopeInputError.AliasError.TooShort>()
                }

                it("should fail with more than 64 characters") {
                    val longName = "a".repeat(65)
                    val result = AliasName.create(longName)
                    result.shouldBeLeft()
                    result.leftOrNull()?.shouldBeInstanceOf<ScopeInputError.AliasError.TooLong>()
                }

                it("should fail when starting with hyphen") {
                    val result = AliasName.create("-invalid")
                    result.shouldBeLeft()
                    result.leftOrNull()?.shouldBeInstanceOf<ScopeInputError.AliasError.InvalidFormat>()
                }

                it("should fail when ending with hyphen") {
                    val result = AliasName.create("invalid-")
                    result.shouldBeLeft()
                    result.leftOrNull()?.shouldBeInstanceOf<ScopeInputError.AliasError.InvalidFormat>()
                }

                it("should fail when starting with underscore") {
                    val result = AliasName.create("_invalid")
                    result.shouldBeLeft()
                    result.leftOrNull()?.shouldBeInstanceOf<ScopeInputError.AliasError.InvalidFormat>()
                }

                it("should fail when ending with underscore") {
                    val result = AliasName.create("invalid_")
                    result.shouldBeLeft()
                    result.leftOrNull()?.shouldBeInstanceOf<ScopeInputError.AliasError.InvalidFormat>()
                }

                it("should fail with special characters") {
                    val invalidNames = listOf(
                        "invalid@name",
                        "invalid!name",
                        "invalid#name",
                        "invalid\$name",
                        "invalid%name",
                        "invalid^name",
                        "invalid&name",
                        "invalid*name",
                        "invalid(name",
                        "invalid)name",
                        "invalid+name",
                        "invalid=name",
                        "invalid[name",
                        "invalid]name",
                        "invalid{name",
                        "invalid}name",
                        "invalid|name",
                        "invalid\\name",
                        "invalid:name",
                        "invalid;name",
                        "invalid\"name",
                        "invalid'name",
                        "invalid<name",
                        "invalid>name",
                        "invalid,name",
                        "invalid.name",
                        "invalid?name",
                        "invalid/name",
                        "invalid~name",
                        "invalid`name",
                        "invalid name", // space
                    )

                    invalidNames.forEach { invalidName ->
                        val result = AliasName.create(invalidName)
                        result.shouldBeLeft()
                        result.leftOrNull()?.shouldBeInstanceOf<ScopeInputError.AliasError.InvalidFormat>()
                    }
                }

                it("should fail with consecutive special characters") {
                    val invalidNames = listOf(
                        "invalid--name",
                        "invalid__name",
                        "invalid-_name",
                        "invalid_-name",
                    )

                    invalidNames.forEach { invalidName ->
                        val result = AliasName.create(invalidName)
                        result.shouldBeLeft()
                        result.leftOrNull()?.shouldBeInstanceOf<ScopeInputError.AliasError.InvalidFormat>()
                    }
                }

                it("should trim input and create valid alias") {
                    val result = AliasName.create("  validName  ")
                    result.shouldBeRight()
                    result.getOrNull()?.value shouldBe "validName"
                }
            }

            describe("property-based tests") {
                it("should accept all valid alias names") {
                    checkAll(validAliasNameArb) { validName ->
                        val result = AliasName.create(validName)
                        result.shouldBeRight()
                        result.getOrNull()?.value shouldBe validName
                    }
                }

                it("should reject all invalid alias names") {
                    checkAll(invalidAliasNameArb) { invalidName ->
                        val result = AliasName.create(invalidName)
                        result.shouldBeLeft()
                    }
                }
            }

            describe("toString") {
                it("should return the alias name value") {
                    val aliasName = AliasName.create("testAlias").getOrNull()!!
                    aliasName.toString() shouldBe "testAlias"
                }
            }

            describe("equals and hashCode") {
                it("should be equal for same alias name values") {
                    val aliasName1 = AliasName.create("testAlias").getOrNull()!!
                    val aliasName2 = AliasName.create("testAlias").getOrNull()!!

                    (aliasName1 == aliasName2) shouldBe true
                    aliasName1.hashCode() shouldBe aliasName2.hashCode()
                }

                it("should not be equal for different alias name values") {
                    val aliasName1 = AliasName.create("testAlias1").getOrNull()!!
                    val aliasName2 = AliasName.create("testAlias2").getOrNull()!!

                    (aliasName1 == aliasName2) shouldBe false
                }
            }
        }
    })

// Property-based testing generators
private val validAliasNameArb = Arb.string(2..64).filter { str ->
    str.matches(Regex("^[a-zA-Z0-9]([a-zA-Z0-9_-]*[a-zA-Z0-9])?$")) &&
        !str.contains(Regex("[-_]{2,}"))
}

private val invalidAliasNameArb = Arb.string(0..100).filter { str ->
    str.isBlank() ||
        str.length < 2 ||
        str.length > 64 ||
        !str.matches(Regex("^[a-zA-Z0-9]([a-zA-Z0-9_-]*[a-zA-Z0-9])?$")) ||
        str.contains(Regex("[-_]{2,}"))
}
