package io.github.kamiazya.scopes.apps.cli.integration

import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.forAll

class AspectValuePropertyTest :
    DescribeSpec({
        describe("AspectValue Property Tests") {

            describe("Value Creation Properties") {
                it("should create aspect values for non-empty strings") {
                    forAll(Arb.string(1, 50).filterNot { it.isBlank() }) { input ->
                        val result = AspectValue.create(input)
                        result.isRight()
                    }
                }

                it("should preserve original values") {
                    forAll(Arb.string(1, 50).filterNot { it.isBlank() }) { input ->
                        val result = AspectValue.create(input)
                        result.fold(
                            { false },
                            { it.value == input },
                        )
                    }
                }
            }

            describe("Numeric Properties") {
                it("should validate numeric strings correctly") {
                    val validNumbers = listOf("42", "3.14", "-10", "0", "1.5")
                    validNumbers.forEach { numStr ->
                        val aspectValue = AspectValue.create(numStr).getOrNull()!!
                        aspectValue.isNumeric() shouldBe true
                    }
                }

                it("should reject invalid numeric strings") {
                    val invalidNumbers = listOf("abc", "1.2.3", "not-a-number")
                    invalidNumbers.forEach { invalidStr ->
                        val aspectValue = AspectValue.create(invalidStr).getOrNull()!!
                        aspectValue.isNumeric() shouldBe false
                    }
                }
            }

            describe("Boolean Properties") {
                it("should validate true-like values") {
                    val trueValues = listOf("true", "yes", "1", "TRUE", "Yes")
                    trueValues.forEach { boolStr ->
                        val aspectValue = AspectValue.create(boolStr).getOrNull()!!
                        aspectValue.isBoolean() shouldBe true
                        aspectValue.toBooleanValue() shouldBe true
                    }
                }

                it("should validate false-like values") {
                    val falseValues = listOf("false", "no", "0", "FALSE", "No")
                    falseValues.forEach { boolStr ->
                        val aspectValue = AspectValue.create(boolStr).getOrNull()!!
                        aspectValue.isBoolean() shouldBe true
                        aspectValue.toBooleanValue() shouldBe false
                    }
                }
            }

            describe("Duration Properties") {
                it("should validate ISO 8601 duration formats") {
                    val validDurations = listOf("P1D", "PT2H", "PT30M", "PT45S", "P1DT2H30M", "P1W")
                    validDurations.forEach { durationStr ->
                        val aspectValue = AspectValue.create(durationStr).getOrNull()!!
                        aspectValue.isDuration() shouldBe true
                    }
                }

                it("should reject invalid duration formats") {
                    val invalidDurations = listOf("1D", "T2H", "2H30M", "1 day", "invalid")
                    invalidDurations.forEach { invalidStr ->
                        val aspectValue = AspectValue.create(invalidStr).getOrNull()!!
                        aspectValue.isDuration() shouldBe false
                    }
                }
            }

            describe("Equality Properties") {
                it("should be reflexive") {
                    forAll(Arb.string(1, 50).filterNot { it.isBlank() }) { input ->
                        val value1 = AspectValue.create(input).getOrNull()!!
                        val value2 = AspectValue.create(input).getOrNull()!!
                        value1 == value2
                    }
                }
            }
        }
    })
