package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class AliasTypeTest :
    DescribeSpec({
        describe("AliasType") {
            describe("enum values") {
                it("should have CANONICAL value") {
                    AliasType.CANONICAL shouldNotBe null
                    AliasType.CANONICAL.name shouldBe "CANONICAL"
                }

                it("should have CUSTOM value") {
                    AliasType.CUSTOM shouldNotBe null
                    AliasType.CUSTOM.name shouldBe "CUSTOM"
                }

                it("should have exactly 2 values") {
                    AliasType.values().size shouldBe 2
                }
            }

            describe("valueOf") {
                it("should return CANONICAL for 'CANONICAL'") {
                    AliasType.valueOf("CANONICAL") shouldBe AliasType.CANONICAL
                }

                it("should return CUSTOM for 'CUSTOM'") {
                    AliasType.valueOf("CUSTOM") shouldBe AliasType.CUSTOM
                }
            }

            describe("ordinal") {
                it("should have correct ordinal values") {
                    AliasType.CANONICAL.ordinal shouldBe 0
                    AliasType.CUSTOM.ordinal shouldBe 1
                }
            }

            describe("equality") {
                it("should be equal to itself") {
                    (AliasType.CANONICAL == AliasType.CANONICAL) shouldBe true
                    (AliasType.CUSTOM == AliasType.CUSTOM) shouldBe true
                }

                it("should not be equal to different type") {
                    (AliasType.CANONICAL == AliasType.CUSTOM) shouldBe false
                }
            }
        }
    })
