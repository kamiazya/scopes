package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class AspectValueDurationTest :
    DescribeSpec({
        describe("AspectValue ISO 8601 Duration Parsing") {

            describe("Valid durations") {
                it("should parse simple day duration P1D") {
                    val aspectValue = AspectValue.create("P1D").getOrNull()!!
                    aspectValue.isDuration() shouldBe true
                    aspectValue.parseDuration() shouldBe 1.days
                }

                it("should parse multiple days P3D") {
                    val aspectValue = AspectValue.create("P3D").getOrNull()!!
                    aspectValue.isDuration() shouldBe true
                    aspectValue.parseDuration() shouldBe 3.days
                }

                it("should parse simple time duration PT2H") {
                    val aspectValue = AspectValue.create("PT2H").getOrNull()!!
                    aspectValue.isDuration() shouldBe true
                    aspectValue.parseDuration() shouldBe 2.hours
                }

                it("should parse time with minutes PT30M") {
                    val aspectValue = AspectValue.create("PT30M").getOrNull()!!
                    aspectValue.isDuration() shouldBe true
                    aspectValue.parseDuration() shouldBe 30.minutes
                }

                it("should parse time with seconds PT45S") {
                    val aspectValue = AspectValue.create("PT45S").getOrNull()!!
                    aspectValue.isDuration() shouldBe true
                    aspectValue.parseDuration() shouldBe 45.seconds
                }

                it("should parse complex time PT2H30M45S") {
                    val aspectValue = AspectValue.create("PT2H30M45S").getOrNull()!!
                    aspectValue.isDuration() shouldBe true
                    val expected = 2.hours + 30.minutes + 45.seconds
                    aspectValue.parseDuration() shouldBe expected
                }

                it("should parse combined date and time P1DT2H") {
                    val aspectValue = AspectValue.create("P1DT2H").getOrNull()!!
                    aspectValue.isDuration() shouldBe true
                    aspectValue.parseDuration() shouldBe (1.days + 2.hours)
                }

                it("should parse complex combined P2DT3H4M5S") {
                    val aspectValue = AspectValue.create("P2DT3H4M5S").getOrNull()!!
                    aspectValue.isDuration() shouldBe true
                    val expected = 2.days + 3.hours + 4.minutes + 5.seconds
                    aspectValue.parseDuration() shouldBe expected
                }

                it("should parse week duration P1W") {
                    val aspectValue = AspectValue.create("P1W").getOrNull()!!
                    aspectValue.isDuration() shouldBe true
                    aspectValue.parseDuration() shouldBe 7.days
                }

                it("should parse multiple weeks P2W") {
                    val aspectValue = AspectValue.create("P2W").getOrNull()!!
                    aspectValue.isDuration() shouldBe true
                    aspectValue.parseDuration() shouldBe 14.days
                }

                it("should parse fractional seconds PT0.5S") {
                    val aspectValue = AspectValue.create("PT0.5S").getOrNull()!!
                    aspectValue.isDuration() shouldBe true
                    aspectValue.parseDuration() shouldBe 500.milliseconds
                }

                it("should parse fractional minutes PT1.5M") {
                    val aspectValue = AspectValue.create("PT1.5M").getOrNull()!!
                    aspectValue.isDuration() shouldBe true
                    aspectValue.parseDuration() shouldBe 90.seconds
                }

                it("should parse fractional hours PT1.5H") {
                    val aspectValue = AspectValue.create("PT1.5H").getOrNull()!!
                    aspectValue.isDuration() shouldBe true
                    aspectValue.parseDuration() shouldBe 90.minutes
                }

                it("should parse complex fractional duration PT2.5H30.5M10.5S") {
                    val aspectValue = AspectValue.create("PT2.5H30.5M10.5S").getOrNull()!!
                    aspectValue.isDuration() shouldBe true
                    // 2.5 hours = 150 minutes = 9000 seconds
                    // 30.5 minutes = 1830 seconds
                    // 10.5 seconds = 10.5 seconds
                    // Total = 10840.5 seconds = 10840500 milliseconds
                    aspectValue.parseDuration() shouldBe 10840500.milliseconds
                }

                it("should truncate sub-millisecond precision PT0.0001S") {
                    val aspectValue = AspectValue.create("PT0.0001S").getOrNull()!!
                    aspectValue.isDuration() shouldBe true
                    // 0.0001 seconds = 0.1 milliseconds, truncated to 0
                    // This results in 0, which fails the non-zero check
                    aspectValue.parseDuration() shouldBe null
                }

                it("should preserve millisecond precision PT0.001S") {
                    val aspectValue = AspectValue.create("PT0.001S").getOrNull()!!
                    aspectValue.isDuration() shouldBe true
                    // 0.001 seconds = 1 millisecond, preserved
                    aspectValue.parseDuration() shouldBe 1.milliseconds
                }

                it("should truncate to nearest millisecond PT0.0015S") {
                    val aspectValue = AspectValue.create("PT0.0015S").getOrNull()!!
                    aspectValue.isDuration() shouldBe true
                    // 0.0015 seconds = 1.5 milliseconds, truncated to 1 millisecond
                    aspectValue.parseDuration() shouldBe 1.milliseconds
                }
            }

            describe("Invalid durations - missing P prefix") {
                it("should reject duration without P prefix: 1D") {
                    val aspectValue = AspectValue.create("1D").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }

                it("should reject duration without P prefix: T2H") {
                    val aspectValue = AspectValue.create("T2H").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }
            }

            describe("Invalid durations - missing T separator for time") {
                it("should reject time components without T separator: P2H") {
                    val aspectValue = AspectValue.create("P2H").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }

                it("should reject mixed date/time without T separator: P1D2H") {
                    val aspectValue = AspectValue.create("P1D2H").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }

                it("should reject minutes without T: P30M") {
                    val aspectValue = AspectValue.create("P30M").getOrNull()!!
                    // This is ambiguous - could be months or minutes
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }

                it("should reject seconds without T: P45S") {
                    val aspectValue = AspectValue.create("P45S").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }
            }

            describe("Invalid durations - wrong order") {
                it("should reject date components in wrong order: P1MD1") {
                    // Days should come before months in date part
                    val aspectValue = AspectValue.create("P1MD1").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }

                it("should reject time components in wrong order: PT1M2H") {
                    val aspectValue = AspectValue.create("PT1M2H").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }

                it("should reject seconds before hours: PT30S2H") {
                    val aspectValue = AspectValue.create("PT30S2H").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }

                it("should reject time before date: PT2HP1D") {
                    val aspectValue = AspectValue.create("PT2HP1D").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }
            }

            describe("Invalid durations - unsupported units") {
                it("should reject year durations: P1Y") {
                    val aspectValue = AspectValue.create("P1Y").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }

                it("should reject month durations: P1M") {
                    val aspectValue = AspectValue.create("P1M").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }

                it("should reject mixed year/month/day: P1Y2M3D") {
                    val aspectValue = AspectValue.create("P1Y2M3D").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }
            }

            describe("Invalid durations - malformed") {
                it("should reject empty duration: P") {
                    val aspectValue = AspectValue.create("P").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }

                it("should reject only T separator: PT") {
                    val aspectValue = AspectValue.create("PT").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }

                it("should reject invalid characters: PABC") {
                    val aspectValue = AspectValue.create("PABC").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }

                it("should reject negative values: P-1D") {
                    val aspectValue = AspectValue.create("P-1D").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }

                it("should reject week mixed with other components: P1W2D") {
                    val aspectValue = AspectValue.create("P1W2D").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }

                it("should reject week with time components: P1WT2H") {
                    val aspectValue = AspectValue.create("P1WT2H").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }
            }

            describe("Edge cases") {
                it("should handle zero values: P0D") {
                    val aspectValue = AspectValue.create("P0D").getOrNull()!!
                    // Zero duration is technically valid in ISO 8601, but our implementation rejects it
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }

                it("should handle zero weeks: P0W") {
                    val aspectValue = AspectValue.create("P0W").getOrNull()!!
                    // P0W should be rejected consistently by both isDuration and parseDuration
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }

                it("should handle very large values: P365D") {
                    val aspectValue = AspectValue.create("P365D").getOrNull()!!
                    aspectValue.isDuration() shouldBe true
                    aspectValue.parseDuration() shouldBe 365.days
                }

                it("should handle components without values as invalid: PD") {
                    val aspectValue = AspectValue.create("PD").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }

                it("should handle components without values in time: PTH") {
                    val aspectValue = AspectValue.create("PTH").getOrNull()!!
                    aspectValue.isDuration() shouldBe false
                    aspectValue.parseDuration() shouldBe null
                }
            }
        }
    })
