package io.github.kamiazya.scopes.domain.valueobject

import io.github.kamiazya.scopes.domain.error.ScopeInputError
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.kotest.property.forAll

class ScopeTitlePropertyTest : StringSpec({

    "valid scope titles should always be created successfully" {
        checkAll(validScopeTitleArb()) { title ->
            val result = ScopeTitle.create(title)
            result.isRight() shouldBe true
            result.getOrNull()?.value shouldBe title.trim()
        }
    }

    "scope titles should reject empty strings" {
        checkAll(emptyStringArb()) { title ->
            val result = ScopeTitle.create(title)
            result.isLeft() shouldBe true
            val error = result.leftOrNull()
            // Can be either Empty or ContainsProhibitedCharacters if the string contains newlines
            (error is ScopeInputError.TitleError.Empty ||
             error is ScopeInputError.TitleError.ContainsProhibitedCharacters) shouldBe true
        }
    }

    "scope titles should reject strings that are too long" {
        checkAll(tooLongStringArb()) { title ->
            val result = ScopeTitle.create(title)
            result.isLeft() shouldBe true
            val error = result.leftOrNull()
            // Check that it's the too long error
            error shouldBe instanceOf<ScopeInputError.TitleError.TooLong>()
        }
    }

    "scope titles should reject strings with newline characters" {
        checkAll(stringWithNewlinesArb()) { title ->
            // Only test if trimmed title is not blank and within length limits
            val trimmed = title.trim()
            if (trimmed.isNotEmpty() && !trimmed.contains('\n') && !trimmed.contains('\r') && trimmed.length <= ScopeTitle.MAX_LENGTH) {
                // This would be a valid title without newlines, skip
            } else if (trimmed.isNotEmpty() && trimmed.length <= ScopeTitle.MAX_LENGTH && (title.contains('\n') || title.contains('\r'))) {
                val result = ScopeTitle.create(title)
                result.isLeft() shouldBe true
                result.leftOrNull() shouldBe instanceOf<ScopeInputError.TitleError.ContainsProhibitedCharacters>()
            }
        }
    }

    "scope title creation should be idempotent" {
        checkAll(validScopeTitleArb()) { title ->
            val result1 = ScopeTitle.create(title)
            val result2 = ScopeTitle.create(title)
            result1 shouldBe result2
        }
    }

    "scope titles with leading/trailing spaces should be trimmed" {
        checkAll(validScopeTitleArb()) { title ->
            val withSpaces = "  $title  "
            val result = ScopeTitle.create(withSpaces)
            result.isRight() shouldBe true
            result.getOrNull()?.value shouldBe title.trim()
        }
    }

    "scope title string representation should equal its value" {
        checkAll(validScopeTitleArb()) { title ->
            val scopeTitle = ScopeTitle.create(title).getOrNull()
            scopeTitle?.toString() shouldBe title.trim()
        }
    }

    "scope titles preserve case sensitivity" {
        forAll(
            Arb.string(1..100)
                .filter { it.trim().isNotEmpty() && !it.contains('\n') && !it.contains('\r') }
        ) { title ->
            val scopeTitle = ScopeTitle.create(title).getOrNull()
            scopeTitle?.value == title.trim()
        }
    }

    "scope titles can contain special characters except newlines" {
        checkAll(
            Arb.string(1..100)
                .filter { it.trim().isNotEmpty() && !it.contains('\n') && !it.contains('\r') }
        ) { title ->
            val result = ScopeTitle.create(title)
            result.isRight() shouldBe true
            result.getOrNull()?.value shouldBe title.trim()
        }
    }

    "scope titles should handle unicode characters" {
        checkAll(
            Arb.string(1..100)
                .filter { it.trim().isNotEmpty() && it.trim().length <= ScopeTitle.MAX_LENGTH && !it.contains('\n') && !it.contains('\r') }
        ) { title ->
            val result = ScopeTitle.create(title)
            result.isRight() shouldBe true
            result.getOrNull()?.value shouldBe title.trim()
        }
    }

    "scope titles with only whitespace should be rejected" {
        checkAll(Arb.of(" ", "  ", "\t", "\t\t", "   \t   ")) { title ->
            val result = ScopeTitle.create(title)
            result.isLeft() shouldBe true
            result.leftOrNull() shouldBe instanceOf<ScopeInputError.TitleError.Empty>()
        }
    }

    "scope titles at boundary lengths should be handled correctly" {
        // Exactly at max length
        val maxLengthTitle = "a".repeat(ScopeTitle.MAX_LENGTH)
        ScopeTitle.create(maxLengthTitle).isRight() shouldBe true

        // One over max length
        val overMaxTitle = "a".repeat(ScopeTitle.MAX_LENGTH + 1)
        ScopeTitle.create(overMaxTitle).isLeft() shouldBe true

        // Exactly at min length
        val minLengthTitle = "a".repeat(ScopeTitle.MIN_LENGTH)
        ScopeTitle.create(minLengthTitle).isRight() shouldBe true
    }

    "scope titles should handle tabs and spaces consistently" {
        checkAll(
            Arb.string(1..50)
                .filter { it.trim().isNotEmpty() }
                .map { "\t$it\t" }
        ) { title ->
            val result = ScopeTitle.create(title)
            result.isRight() shouldBe true
            result.getOrNull()?.value shouldBe title.trim()
        }
    }

    "scope titles with mixed whitespace should be trimmed correctly" {
        checkAll(validScopeTitleArb()) { title ->
            val withMixedWhitespace = " \t $title \t "
            val result = ScopeTitle.create(withMixedWhitespace)
            result.isRight() shouldBe true
            result.getOrNull()?.value shouldBe title.trim()
        }
    }
})

// Custom Arbitrary generators
private fun validScopeTitleArb(): Arb<String> = Arb.string(
    minSize = ScopeTitle.MIN_LENGTH,
    maxSize = ScopeTitle.MAX_LENGTH
).filter { it.trim().isNotEmpty() && !it.contains('\n') && !it.contains('\r') }

private fun emptyStringArb(): Arb<String> = Arb.of("", " ", "  ", "\t", "\n", "\r", "   \t   ")

private fun tooLongStringArb(): Arb<String> = Arb.string(
    minSize = ScopeTitle.MAX_LENGTH + 1,
    maxSize = ScopeTitle.MAX_LENGTH + 100
).filter { it.trim().length > ScopeTitle.MAX_LENGTH }

private fun stringWithNewlinesArb(): Arb<String> = Arb.choice(
    Arb.string(1..50).map { "$it\n" },
    Arb.string(1..50).map { "\n$it" },
    Arb.string(1..50).map { it.take(it.length / 2) + "\n" + it.drop(it.length / 2) },
    Arb.string(1..50).map { "$it\r" },
    Arb.string(1..50).map { "\r$it" },
    Arb.string(1..50).map { it.take(it.length / 2) + "\r" + it.drop(it.length / 2) },
    Arb.string(1..50).map { "$it\r\n" }
)
