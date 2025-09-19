package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.error.AspectValueError
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for AspectValue value object.
 *
 * Business rules:
 * - Value cannot be blank/empty
 * - Value cannot exceed 1000 characters
 * - Supports numeric, boolean, and duration parsing
 * - Implements comparison operators for different value types
 * - Duration parsing follows ISO 8601 standard with specific restrictions
 */
class AspectValueTest :
    StringSpec({

        "should create AspectValue with valid string" {
            val result = AspectValue.create("test-value")

            result shouldBe AspectValue.unsafeCreate("test-value").right()
            result.getOrNull()?.value shouldBe "test-value"
        }

        "should create AspectValue with whitespace-only string as valid" {
            val result = AspectValue.create("   ")

            result.isLeft() shouldBe true
            result.leftOrNull() shouldBe AspectValueError.EmptyValue
        }

        "should fail to create AspectValue with empty string" {
            val result = AspectValue.create("")

            result shouldBe AspectValueError.EmptyValue.left()
        }

        "should fail to create AspectValue with blank string" {
            val result = AspectValue.create("   ")

            result shouldBe AspectValueError.EmptyValue.left()
        }

        "should fail to create AspectValue with too long string" {
            val longValue = "a".repeat(1001)
            val result = AspectValue.create(longValue)

            result.isLeft() shouldBe true
            val error = result.leftOrNull()
            error.shouldBeInstanceOf<AspectValueError.TooLong>()
            error.actualLength shouldBe 1001
            error.maxLength shouldBe 1000
        }

        "should create AspectValue with maximum allowed length" {
            val maxValue = "a".repeat(1000)
            val result = AspectValue.create(maxValue)

            result.isRight() shouldBe true
            result.getOrNull()?.value?.length shouldBe 1000
        }

        "should create unsafe AspectValue without validation" {
            val value = AspectValue.unsafeCreate("")

            value.value shouldBe ""
        }

        "should detect numeric values correctly" {
            val numericValues = listOf("123", "45.67", "0", "-89", "0.001", "1e10")
            val nonNumericValues = listOf("abc", "12.34.56", "text123", "123text", "")

            numericValues.forEach { valueStr ->
                val value = AspectValue.unsafeCreate(valueStr)
                value.isNumeric() shouldBe true
                value.toNumericValue() shouldBe valueStr.toDouble()
            }

            nonNumericValues.forEach { valueStr ->
                val value = AspectValue.unsafeCreate(valueStr)
                value.isNumeric() shouldBe false
                value.toNumericValue() shouldBe null
            }
        }

        "should detect boolean values correctly" {
            val trueBooleans = listOf("true", "TRUE", "True", "yes", "YES", "Yes", "1")
            val falseBooleans = listOf("false", "FALSE", "False", "no", "NO", "No", "0")
            val nonBooleans = listOf("maybe", "2", "on", "off", "")

            trueBooleans.forEach { valueStr ->
                val value = AspectValue.unsafeCreate(valueStr)
                value.isBoolean() shouldBe true
                value.parseBoolean() shouldBe true
                value.toBooleanValue() shouldBe true
            }

            falseBooleans.forEach { valueStr ->
                val value = AspectValue.unsafeCreate(valueStr)
                value.isBoolean() shouldBe true
                value.parseBoolean() shouldBe false
                value.toBooleanValue() shouldBe false
            }

            nonBooleans.forEach { valueStr ->
                val value = AspectValue.unsafeCreate(valueStr)
                value.isBoolean() shouldBe false
                value.parseBoolean() shouldBe null
                value.toBooleanValue() shouldBe null
            }
        }

        "should compare numeric values correctly" {
            val value1 = AspectValue.unsafeCreate("10")
            val value2 = AspectValue.unsafeCreate("20")
            val value3 = AspectValue.unsafeCreate("10")

            (value1 < value2) shouldBe true
            (value2 > value1) shouldBe true
            (value1 == value3) shouldBe true
            value1.compareTo(value2) shouldBe -1
            value2.compareTo(value1) shouldBe 1
            value1.compareTo(value3) shouldBe 0
        }

        "should compare boolean values correctly" {
            val trueValue = AspectValue.unsafeCreate("true")
            val falseValue = AspectValue.unsafeCreate("false")
            val anotherTrue = AspectValue.unsafeCreate("yes")

            (falseValue < trueValue) shouldBe true
            (trueValue > falseValue) shouldBe true
            trueValue.compareTo(anotherTrue) shouldBe 0
        }

        "should fall back to string comparison for non-numeric/non-boolean values" {
            val value1 = AspectValue.unsafeCreate("apple")
            val value2 = AspectValue.unsafeCreate("banana")
            val value3 = AspectValue.unsafeCreate("apple")

            (value1 < value2) shouldBe true
            (value2 > value1) shouldBe true
            value1.compareTo(value3) shouldBe 0
        }

        "should compare mixed types with string comparison fallback" {
            val numericValue = AspectValue.unsafeCreate("123")
            val textValue = AspectValue.unsafeCreate("abc")

            // Falls back to string comparison since types don't match
            numericValue.compareTo(textValue) shouldBe "123".compareTo("abc")
        }

        "should detect valid ISO 8601 duration formats" {
            val validDurations = listOf(
                "P1D", "PT1H", "PT30M", "PT45S", "P1W",
                "P2DT3H", "P1DT2H30M", "PT2H30M", "P1DT2H30M45S",
                "PT0.5H", "PT30.5M", "PT45.123S",
            )

            validDurations.forEach { durationStr ->
                val value = AspectValue.unsafeCreate(durationStr)
                value.isDuration() shouldBe true
            }
        }

        "should detect invalid ISO 8601 duration formats" {
            val invalidDurations = listOf(
                "1D", "P", "PT", "P1Y", "P1M", "P-1D", "P1WT1H",
                "P1H", "PT1D", "P1DT", "abc", "123", "",
            )

            invalidDurations.forEach { durationStr ->
                val value = AspectValue.unsafeCreate(durationStr)
                value.isDuration() shouldBe false
            }
        }

        "should parse simple duration components correctly" {
            val testCases = mapOf(
                "P1D" to 1.days,
                "PT1H" to 1.hours,
                "PT30M" to 30.minutes,
                "PT45S" to 45.seconds,
                "P1W" to 7.days,
            )

            testCases.forEach { (durationStr, expectedDuration) ->
                val value = AspectValue.unsafeCreate(durationStr)
                value.parseDuration() shouldBe expectedDuration
            }
        }

        "should parse complex duration combinations correctly" {
            val testCases = mapOf(
                "P2DT3H" to (2.days + 3.hours),
                "P1DT2H30M" to (1.days + 2.hours + 30.minutes),
                "PT2H30M45S" to (2.hours + 30.minutes + 45.seconds),
                "P1DT2H30M45S" to (1.days + 2.hours + 30.minutes + 45.seconds),
            )

            testCases.forEach { (durationStr, expectedDuration) ->
                val value = AspectValue.unsafeCreate(durationStr)
                value.parseDuration() shouldBe expectedDuration
            }
        }

        "should parse fractional duration components correctly" {
            val testCases = mapOf(
                "PT0.5H" to 30.minutes,
                "PT30.5M" to (30.minutes + 30.seconds),
                "PT45.123S" to (45.seconds + 123.milliseconds),
            )

            testCases.forEach { (durationStr, expectedDuration) ->
                val value = AspectValue.unsafeCreate(durationStr)
                value.parseDuration() shouldBe expectedDuration
            }
        }

        "should reject unsupported duration components" {
            val unsupportedDurations = listOf("P1Y", "P1M", "P1YT1H", "P1MT1H")

            unsupportedDurations.forEach { durationStr ->
                val value = AspectValue.unsafeCreate(durationStr)
                value.isDuration() shouldBe false
                value.parseDuration() shouldBe null
            }
        }

        "should reject negative duration components" {
            val negativeDurations = listOf("P-1D", "PT-1H", "PT-30M", "PT-45S")

            negativeDurations.forEach { durationStr ->
                val value = AspectValue.unsafeCreate(durationStr)
                value.isDuration() shouldBe false
                value.parseDuration() shouldBe null
            }
        }

        "should reject week durations combined with other components" {
            val invalidCombinations = listOf("P1WT1H", "P1W1D", "P1WT30M")

            invalidCombinations.forEach { durationStr ->
                val value = AspectValue.unsafeCreate(durationStr)
                value.isDuration() shouldBe false
                value.parseDuration() shouldBe null
            }
        }

        "should reject duration with time components before T separator" {
            val invalidPlacements = listOf("P1H", "P30M", "P45S", "P1D2H")

            invalidPlacements.forEach { durationStr ->
                val value = AspectValue.unsafeCreate(durationStr)
                value.isDuration() shouldBe false
                value.parseDuration() shouldBe null
            }
        }

        "should reject duration with empty time part after T" {
            val value = AspectValue.unsafeCreate("P1DT")
            value.isDuration() shouldBe false
            value.parseDuration() shouldBe null
        }

        "should reject duration with all zero components" {
            val zeroDurations = listOf("P0D", "PT0H", "PT0M", "PT0S", "P0DT0H0M0S")

            zeroDurations.forEach { durationStr ->
                val value = AspectValue.unsafeCreate(durationStr)
                value.isDuration() shouldBe false
                value.parseDuration() shouldBe null
            }
        }

        "should handle millisecond precision truncation correctly" {
            // Test that sub-millisecond precision is truncated
            val value = AspectValue.unsafeCreate("PT0.0001S") // 0.1ms

            // The implementation accepts this format as valid but truncates to 0ms during parsing
            value.isDuration() shouldBe true
            value.parseDuration() shouldBe null // Truncated to 0ms, so returns null
        }

        "should preserve millisecond precision" {
            val value = AspectValue.unsafeCreate("PT0.001S") // 1ms

            value.isDuration() shouldBe true
            value.parseDuration() shouldBe 1.milliseconds
        }

        "should convert toString correctly" {
            val value = AspectValue.unsafeCreate("test-value")
            value.toString() shouldBe "test-value"
        }

        "should maintain value equality" {
            val value1 = AspectValue.unsafeCreate("same-value")
            val value2 = AspectValue.unsafeCreate("same-value")
            val value3 = AspectValue.unsafeCreate("different-value")

            (value1 == value2) shouldBe true
            (value1 == value3) shouldBe false
            value1.hashCode() shouldBe value2.hashCode()
        }

        "should handle edge cases in duration parsing" {
            // Valid minimal duration
            val minDuration = AspectValue.unsafeCreate("PT0.001S")
            minDuration.isDuration() shouldBe true
            minDuration.parseDuration() shouldBe 1.milliseconds

            // Complex valid duration
            val complexDuration = AspectValue.unsafeCreate("P7DT23H59M59.999S")
            complexDuration.isDuration() shouldBe true

            // Invalid format variations
            val invalidFormats = listOf(
                "P", // Missing components
                "PT", // Missing time components
                "P1DT", // T without time components
                "1D", // Missing P prefix
                "PD1", // Wrong order
                "P1.5D", // Fractional days not supported in this format
            )

            invalidFormats.forEach { format ->
                val value = AspectValue.unsafeCreate(format)
                value.isDuration() shouldBe false
            }
        }

        "should handle numeric edge cases correctly" {
            val edgeCases = mapOf(
                "0" to 0.0,
                "-0" to -0.0,
                "1e10" to 1e10,
                "1E-10" to 1E-10,
                "123.456789" to 123.456789,
                "999999999999" to 999999999999.0,
            )

            edgeCases.forEach { (valueStr, expectedNum) ->
                val value = AspectValue.unsafeCreate(valueStr)
                value.isNumeric() shouldBe true
                value.toNumericValue() shouldBe expectedNum
            }
        }

        "should handle comparison edge cases correctly" {
            // Compare different numeric formats
            val int1 = AspectValue.unsafeCreate("42")
            val float1 = AspectValue.unsafeCreate("42.0")
            val scientific = AspectValue.unsafeCreate("4.2e1")

            int1.compareTo(float1) shouldBe 0
            int1.compareTo(scientific) shouldBe 0

            // Compare boolean aliases
            val true1 = AspectValue.unsafeCreate("true")
            val true2 = AspectValue.unsafeCreate("yes")
            val true3 = AspectValue.unsafeCreate("1")

            true1.compareTo(true2) shouldBe 0
            true2.compareTo(true3) shouldBe 0
        }
    })
