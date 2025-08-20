package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.domain.error.ScopeInputError
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.constant
import io.kotest.property.checkAll

class ScopeDescriptionPropertyTest : StringSpec({

    "valid descriptions should create valid ScopeDescriptions" {
        checkAll(validDescriptionArb()) { description ->
            val result = ScopeDescription.create(description)
            result.isRight() shouldBe true
            result.fold(
                { throw AssertionError("Expected Right but got Left: $it") },
                { scopeDescription ->
                    scopeDescription!!.value shouldBe description.trim()
                }
            )
        }
    }

    "null input should return null ScopeDescription" {
        val result = ScopeDescription.create(null)
        result.isRight() shouldBe true
        result.fold(
            { throw AssertionError("Expected Right but got Left") },
            { scopeDescription ->
                scopeDescription.shouldBeNull()
            }
        )
    }

    "blank strings should return null ScopeDescription" {
        checkAll(blankStringArb()) { blank ->
            val result = ScopeDescription.create(blank)
            result.isRight() shouldBe true
            result.fold(
                { throw AssertionError("Expected Right but got Left") },
                { scopeDescription ->
                    scopeDescription.shouldBeNull()
                }
            )
        }
    }

    "descriptions exceeding max length should return error" {
        checkAll(tooLongDescriptionArb()) { longDescription ->
            val result = ScopeDescription.create(longDescription)
            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    when (error) {
                        is ScopeInputError.DescriptionError.TooLong -> {
                            error.attemptedValue shouldBe longDescription
                            error.maximumLength shouldBe ScopeDescription.MAX_LENGTH
                        }
                        else -> throw AssertionError("Expected TooLong but got $error")
                    }
                },
                { throw AssertionError("Expected Left but got Right") }
            )
        }
    }

    "descriptions should be trimmed" {
        checkAll(descriptionWithWhitespaceArb()) { (original, trimmed) ->
            val result = ScopeDescription.create(original)
            result.isRight() shouldBe true
            result.fold(
                { throw AssertionError("Expected Right but got Left") },
                { scopeDescription ->
                    if (trimmed.isEmpty()) {
                        scopeDescription.shouldBeNull()
                    } else {
                        scopeDescription!!.value shouldBe trimmed
                    }
                }
            )
        }
    }

    "description string representation should equal its value" {
        checkAll(validDescriptionArb()) { description ->
            val result = ScopeDescription.create(description)
            result.isRight() shouldBe true
            result.fold(
                { throw AssertionError("Expected Right but got Left") },
                { scopeDescription ->
                    scopeDescription!!.toString() shouldBe description.trim()
                }
            )
        }
    }

    "descriptions at exactly max length should be valid" {
        val maxLengthDescription = "a".repeat(ScopeDescription.MAX_LENGTH)
        val result = ScopeDescription.create(maxLengthDescription)
        result.isRight() shouldBe true
        result.fold(
            { throw AssertionError("Expected Right but got Left") },
            { scopeDescription ->
                scopeDescription!!.value shouldBe maxLengthDescription
                scopeDescription.value.length shouldBe ScopeDescription.MAX_LENGTH
            }
        )
    }

    "descriptions one character over max length should fail" {
        val overLengthDescription = "a".repeat(ScopeDescription.MAX_LENGTH + 1)
        val result = ScopeDescription.create(overLengthDescription)
        result.isLeft() shouldBe true
        result.fold(
            { error ->
                when (error) {
                    is ScopeInputError.DescriptionError.TooLong -> {
                        error.maximumLength shouldBe ScopeDescription.MAX_LENGTH
                    }
                    else -> throw AssertionError("Expected TooLong but got $error")
                }
            },
            { throw AssertionError("Expected Left but got Right") }
        )
    }

    "description creation should be idempotent for valid inputs" {
        checkAll(validDescriptionArb()) { description ->
            val result1 = ScopeDescription.create(description)
            val result2 = ScopeDescription.create(description)
            
            result1.isRight() shouldBe true
            result2.isRight() shouldBe true
            
            result1.fold(
                { throw AssertionError("Expected Right but got Left") },
                { desc1 ->
                    result2.fold(
                        { throw AssertionError("Expected Right but got Left") },
                        { desc2 ->
                            desc1 shouldBe desc2
                            desc1?.value shouldBe desc2?.value
                        }
                    )
                }
            )
        }
    }

    "descriptions with various unicode characters should be valid" {
        checkAll(unicodeDescriptionArb()) { unicodeDescription ->
            if (unicodeDescription.trim().isNotEmpty() && unicodeDescription.trim().length <= ScopeDescription.MAX_LENGTH) {
                val result = ScopeDescription.create(unicodeDescription)
                result.isRight() shouldBe true
                result.fold(
                    { throw AssertionError("Expected Right but got Left") },
                    { scopeDescription ->
                        scopeDescription!!.value shouldBe unicodeDescription.trim()
                    }
                )
            }
        }
    }

    "multiline descriptions should preserve formatting" {
        checkAll(multilineDescriptionArb()) { multilineDescription ->
            val trimmed = multilineDescription.trim()
            if (trimmed.isNotEmpty() && trimmed.length <= ScopeDescription.MAX_LENGTH) {
                val result = ScopeDescription.create(multilineDescription)
                result.isRight() shouldBe true
                result.fold(
                    { throw AssertionError("Expected Right but got Left") },
                    { scopeDescription ->
                        scopeDescription!!.value shouldBe trimmed
                        // Line breaks should be preserved
                        scopeDescription.value.lines().size shouldBe trimmed.lines().size
                    }
                )
            }
        }
    }

    "different valid descriptions should create different instances" {
        checkAll(
            validDescriptionArb(),
            validDescriptionArb()
        ) { desc1, desc2 ->
            if (desc1.trim() != desc2.trim()) {
                val result1 = ScopeDescription.create(desc1)
                val result2 = ScopeDescription.create(desc2)
                
                result1.isRight() shouldBe true
                result2.isRight() shouldBe true
                
                result1.fold(
                    { throw AssertionError("Expected Right but got Left") },
                    { scopeDesc1 ->
                        result2.fold(
                            { throw AssertionError("Expected Right but got Left") },
                            { scopeDesc2 ->
                                scopeDesc1 shouldNotBe scopeDesc2
                                scopeDesc1?.value shouldNotBe scopeDesc2?.value
                            }
                        )
                    }
                )
            }
        }
    }
})

// Custom Arbitrary generators
private fun validDescriptionArb(): Arb<String> = Arb.string(1..ScopeDescription.MAX_LENGTH)
    .filter { it.trim().isNotEmpty() }

private fun blankStringArb(): Arb<String> = Arb.choice(
    Arb.string().filter { it.isBlank() },
    Arb.choice(listOf("", " ", "  ", "\t", "\n", "\r\n", "   \t   ", "\n\n\n").map { Arb.constant(it) })
)

private fun tooLongDescriptionArb(): Arb<String> = Arb.int(
    ScopeDescription.MAX_LENGTH + 1..ScopeDescription.MAX_LENGTH + 100
).map { length ->
    "a".repeat(length)
}

private fun descriptionWithWhitespaceArb(): Arb<Pair<String, String>> = arbitrary {
    val content = Arb.string(1..100).filter { it.trim().isNotEmpty() }.bind()
    val leadingSpaces = " ".repeat(Arb.int(0..5).bind())
    val trailingSpaces = " ".repeat(Arb.int(0..5).bind())
    val original = leadingSpaces + content + trailingSpaces
    val trimmed = original.trim()
    Pair(original, trimmed)
}

private fun unicodeDescriptionArb(): Arb<String> = Arb.choice(
    // Basic Latin with emojis
    Arb.constant("This is a task with emoji 🚀 and special chars!"),
    // Japanese
    Arb.constant("これは日本語の説明です。タスク管理システム。"),
    // Korean
    Arb.constant("한국어 설명입니다. 작업 관리 시스템."),
    // Chinese
    Arb.constant("这是中文描述。任务管理系统。"),
    // Arabic
    Arb.constant("هذا وصف باللغة العربية. نظام إدارة المهام."),
    // Mixed languages
    Arb.constant("Multi-language: Hello 你好 こんにちは 🌍"),
    // Mathematical symbols
    Arb.constant("Mathematical: ∑ ∏ √ ∞ ≠ ≤ ≥"),
    // Special characters
    Arb.constant("Special chars: ™ © ® € £ ¥")
)

private fun multilineDescriptionArb(): Arb<String> = Arb.choice(
    Arb.constant("Line 1\nLine 2\nLine 3"),
    Arb.constant("Task description:\n- Point 1\n- Point 2\n- Point 3"),
    Arb.constant("First paragraph.\n\nSecond paragraph with double line break."),
    Arb.constant("  Leading spaces\n  on each line\n  should be preserved  "),
    Arb.constant("Windows style\r\nline breaks\r\nshould work"),
    Arb.constant("Mixed\nline\r\nbreaks\n\rformats")
)