package io.github.kamiazya.scopes.domain.valueobject

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.kotest.property.forAll
import io.github.kamiazya.scopes.domain.error.AspectValidationError

class AspectValuePropertyTest : StringSpec({

    "valid aspect values should always be created successfully" {
        checkAll(validAspectValueArb()) { value ->
            val result = AspectValue.create(value)
            result.isRight() shouldBe true
            result.getOrNull()?.value shouldBe value.trim()
        }
    }

    "aspect values should reject empty strings" {
        checkAll(emptyStringArb()) { value ->
            val result = AspectValue.create(value)
            result.isLeft() shouldBe true
            when (result.leftOrNull()) {
                is AspectValidationError.EmptyAspectValue -> true
                else -> false
            } shouldBe true
        }
    }

    "aspect values should reject strings that are too long" {
        checkAll(tooLongStringArb()) { value ->
            val result = AspectValue.create(value)
            result.isLeft() shouldBe true
            when (val error = result.leftOrNull()) {
                is AspectValidationError.AspectValueTooLong -> {
                    error.maxLength shouldBe AspectValue.MAX_LENGTH
                    error.actualLength shouldBe value.trim().length
                    true
                }
                else -> false
            } shouldBe true
        }
    }

    "aspect value creation should be idempotent" {
        checkAll(validAspectValueArb()) { value ->
            val result1 = AspectValue.create(value)
            val result2 = AspectValue.create(value)
            result1 shouldBe result2
        }
    }

    "aspect values with leading/trailing spaces should be trimmed" {
        checkAll(validAspectValueArb()) { value ->
            val withSpaces = "  $value  "
            val result = AspectValue.create(withSpaces)
            result.isRight() shouldBe true
            result.getOrNull()?.value shouldBe value.trim()
        }
    }

    "numeric aspect values should be correctly identified" {
        checkAll(Arb.numericDoubles()) { number ->
            val aspectValue = AspectValue.create(number.toString()).getOrNull()
            aspectValue shouldNotBe null
            aspectValue?.isNumeric() shouldBe true
            aspectValue?.toNumericValue() shouldBe number
        }
    }

    "non-numeric aspect values should not be identified as numeric" {
        checkAll(nonNumericStringArb()) { value ->
            val aspectValue = AspectValue.create(value).getOrNull()
            aspectValue?.isNumeric() shouldBe false
            aspectValue?.toNumericValue() shouldBe null
        }
    }

    "boolean aspect values should be correctly identified" {
        checkAll(Arb.of("true", "false", "True", "False", "TRUE", "FALSE")) { value ->
            val aspectValue = AspectValue.create(value).getOrNull()
            aspectValue shouldNotBe null
            aspectValue?.isBoolean() shouldBe true
            aspectValue?.toBooleanValue() shouldBe value.lowercase().toBoolean()
        }
    }

    "non-boolean aspect values should not be identified as boolean" {
        checkAll(
            Arb.string(1..100)
                .filter { it.trim().isNotEmpty() && it.trim().length <= AspectValue.MAX_LENGTH }
                .filter { it.lowercase() !in listOf("true", "false") }
        ) { value ->
            val aspectValue = AspectValue.create(value).getOrNull()
            aspectValue?.isBoolean() shouldBe false
            aspectValue?.toBooleanValue() shouldBe null
        }
    }

    "aspect value string representation should equal its value" {
        checkAll(validAspectValueArb()) { value ->
            val aspectValue = AspectValue.create(value).getOrNull()
            aspectValue?.toString() shouldBe value.trim()
        }
    }

    "integer values should be preserved as numeric" {
        checkAll(Arb.int(-1000000..1000000)) { number ->
            val aspectValue = AspectValue.create(number.toString()).getOrNull()
            aspectValue shouldNotBe null
            aspectValue?.isNumeric() shouldBe true
            aspectValue?.toNumericValue() shouldBe number.toDouble()
        }
    }

    "aspect values can contain special characters" {
        checkAll(
            Arb.string(1..50)
                .filter { it.trim().isNotEmpty() }
        ) { value ->
            val result = AspectValue.create(value)
            if (value.trim().length <= AspectValue.MAX_LENGTH) {
                result.isRight() shouldBe true
                result.getOrNull()?.value shouldBe value.trim()
            }
        }
    }

    "aspect values preserve case sensitivity" {
        forAll(
            Arb.string(1..50)
                .filter { it.trim().isNotEmpty() }
        ) { value ->
            val aspectValue = AspectValue.create(value).getOrNull()
            aspectValue?.value == value.trim()
        }
    }

    "numeric strings with scientific notation should be identified as numeric" {
        checkAll(Arb.double().filter { it.isFinite() }) { number ->
            val scientific = String.format("%.2e", number)
            val aspectValue = AspectValue.create(scientific).getOrNull()
            aspectValue?.isNumeric() shouldBe true
        }
    }

    "aspect values should handle unicode characters" {
        checkAll(
            Arb.string(1..50)
                .filter { it.trim().isNotEmpty() && it.trim().length <= AspectValue.MAX_LENGTH }
        ) { value ->
            val result = AspectValue.create(value)
            result.isRight() shouldBe true
            result.getOrNull()?.value shouldBe value.trim()
        }
    }
})

// Custom Arbitrary generators
private fun validAspectValueArb(): Arb<String> = Arb.string(
    minSize = AspectValue.MIN_LENGTH,
    maxSize = AspectValue.MAX_LENGTH
).filter { it.trim().isNotEmpty() }

private fun emptyStringArb(): Arb<String> = Arb.of("", " ", "  ", "\t", "\n", "   \t   ")

private fun tooLongStringArb(): Arb<String> = Arb.string(
    minSize = AspectValue.MAX_LENGTH + 1,
    maxSize = AspectValue.MAX_LENGTH + 100
).filter { it.trim().length > AspectValue.MAX_LENGTH }

private fun nonNumericStringArb(): Arb<String> = Arb.string(1..50)
    .filter { it.trim().isNotEmpty() && it.toDoubleOrNull() == null }