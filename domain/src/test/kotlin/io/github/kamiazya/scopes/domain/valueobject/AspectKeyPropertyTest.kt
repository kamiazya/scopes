package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.kotest.property.forAll
import io.github.kamiazya.scopes.domain.error.AspectValidationError

class AspectKeyPropertyTest : StringSpec({

    "valid aspect keys should always be created successfully" {
        checkAll(validAspectKeyArb()) { key ->
            val result = AspectKey.create(key)
            result.isRight() shouldBe true
            result.getOrNull()?.value shouldBe key.trim()
        }
    }

    "aspect keys should reject empty strings" {
        checkAll(emptyStringArb()) { key ->
            val result = AspectKey.create(key)
            result.isLeft() shouldBe true
            when (val error = result.leftOrNull()) {
                is AspectValidationError.EmptyAspectKey -> true
                else -> false
            } shouldBe true
        }
    }

    "aspect keys should reject strings that are too long" {
        checkAll(tooLongStringArb()) { key ->
            val result = AspectKey.create(key)
            result.isLeft() shouldBe true
            when (val error = result.leftOrNull()) {
                is AspectValidationError.AspectKeyTooLong -> {
                    error.maxLength shouldBe AspectKey.MAX_LENGTH
                    error.actualLength shouldBe key.trim().length
                    true
                }
                else -> false
            } shouldBe true
        }
    }

    "aspect keys should reject strings with invalid format" {
        checkAll(invalidFormatArb()) { key ->
            val trimmed = key.trim()
            // Only test if the string is not empty and within length limits
            // to isolate format validation from other validations
            if (trimmed.isNotBlank() && trimmed.length in AspectKey.MIN_LENGTH..AspectKey.MAX_LENGTH) {
                val result = AspectKey.create(key)
                // Check if it matches the valid pattern after trimming
                if (!trimmed.matches(Regex("^[a-z][a-z0-9_]*$"))) {
                    result.isLeft() shouldBe true
                    when (result.leftOrNull()) {
                        is AspectValidationError.InvalidAspectKeyFormat -> true
                        else -> false
                    } shouldBe true
                }
            }
        }
    }

    "aspect key creation should be idempotent" {
        checkAll(validAspectKeyArb()) { key ->
            val result1 = AspectKey.create(key)
            val result2 = AspectKey.create(key)
            result1 shouldBe result2
        }
    }

    "aspect keys with leading/trailing spaces should be trimmed" {
        checkAll(validAspectKeyArb()) { key ->
            val withSpaces = "  $key  "
            val result = AspectKey.create(withSpaces)
            result.isRight() shouldBe true
            result.getOrNull()?.value shouldBe key
        }
    }

    "aspect key string representation should equal its value" {
        checkAll(validAspectKeyArb()) { key ->
            val aspectKey = AspectKey.create(key).getOrNull()
            aspectKey?.toString() shouldBe key.trim()
        }
    }

    "aspect keys should follow the pattern [a-z][a-z0-9_]*" {
        forAll(validAspectKeyArb()) { key ->
            key.matches(Regex("^[a-z][a-z0-9_]*$"))
        }
    }

    "aspect keys starting with uppercase should be rejected" {
        checkAll(Arb.string(1..50).filter { it.isNotEmpty() }) { suffix ->
            val key = "A$suffix"
            val result = AspectKey.create(key)
            if (key.trim().length <= AspectKey.MAX_LENGTH) {
                result.isLeft() shouldBe true
            }
        }
    }

    "aspect keys starting with numbers should be rejected" {
        checkAll(Arb.int(0..9), Arb.stringPattern("[a-z0-9_]{0,49}")) { digit, suffix ->
            val key = "$digit$suffix"
            val result = AspectKey.create(key)
            result.isLeft() shouldBe true
        }
    }
})

// Custom Arbitrary generators for different test scenarios
private fun validAspectKeyArb(): Arb<String> = Arb.stringPattern("[a-z][a-z0-9_]{0,49}")
    .filter { it.length in AspectKey.MIN_LENGTH..AspectKey.MAX_LENGTH }

private fun emptyStringArb(): Arb<String> = Arb.of("", " ", "  ", "\t", "\n", "   \t   ")

private fun tooLongStringArb(): Arb<String> = Arb.string(
    minSize = AspectKey.MAX_LENGTH + 1,
    maxSize = AspectKey.MAX_LENGTH + 100
).map { "a" + it }

private fun invalidFormatArb(): Arb<String> = Arb.choice(
    // Starting with uppercase
    Arb.stringPattern("[A-Z][a-z0-9_]{0,49}"),
    // Starting with number
    Arb.stringPattern("[0-9][a-z0-9_]{0,49}"),
    // Starting with special character
    Arb.string(1..49).map { "_$it" },
    // Contains spaces - ensure we have a space
    Arb.string(1..20).map { base -> "a${base.take(10)} ${base.drop(10)}" },
    // Contains hyphens
    Arb.string(1..20).map { base -> "a${base}-test" },
    // Contains special characters
    Arb.string(1..20).map { base -> "a${base}@test" }
)