package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

class AliasNamePropertyTest :
    StringSpec({

        "valid alias names should create valid AliasName instances" {
            checkAll(validAliasArb()) { aliasString ->
                val result = AliasName.create(aliasString)
                result.isRight() shouldBe true
                result.fold(
                    { throw AssertionError("Expected Right but got Left: $it") },
                    { aliasName ->
                        aliasName.value shouldBe aliasString.lowercase()
                        aliasName.value.matches(Regex("[a-z][a-z0-9-_]{1,63}")) shouldBe true
                    },
                )
            }
        }

        "invalid alias names should fail to create AliasName instances" {
            checkAll(invalidAliasArb()) { invalid ->
                val result = AliasName.create(invalid)
                result.isLeft() shouldBe true
            }
        }

        "empty string should fail to create AliasName" {
            val result = AliasName.create("")
            result.isLeft() shouldBe true
        }

        "blank string should fail to create AliasName" {
            val result = AliasName.create("   ")
            result.isLeft() shouldBe true
        }

        "alias names should be normalized to lowercase" {
            checkAll(mixedCaseValidAliasArb()) { mixedCase ->
                val result = AliasName.create(mixedCase)
                if (result.isRight()) {
                    val aliasName = result.getOrNull()!!
                    aliasName.value shouldBe mixedCase.lowercase()
                }
            }
        }

        "toString should return the normalized value" {
            checkAll(validAliasArb()) { aliasString ->
                val aliasName = AliasName.create(aliasString).getOrNull()!!
                aliasName.toString() shouldBe aliasString.lowercase()
            }
        }

        "equals and hashCode should work correctly" {
            checkAll(validAliasArb()) { aliasString ->
                val aliasName1 = AliasName.create(aliasString).getOrNull()!!
                val aliasName2 = AliasName.create(aliasString).getOrNull()!!
                aliasName1 shouldBe aliasName2
                aliasName1.hashCode() shouldBe aliasName2.hashCode()
            }
        }

        "different AliasNames should not be equal" {
            val aliasName1 = AliasName.create("alias-one").getOrNull()!!
            val aliasName2 = AliasName.create("alias-two").getOrNull()!!
            aliasName1 shouldNotBe aliasName2
        }

        "case insensitive comparison should work correctly" {
            checkAll(mixedCaseValidAliasArb()) { mixedCase ->
                val result1 = AliasName.create(mixedCase)
                val result2 = AliasName.create(mixedCase.lowercase())

                if (result1.isRight() && result2.isRight()) {
                    val aliasName1 = result1.getOrNull()!!
                    val aliasName2 = result2.getOrNull()!!
                    aliasName1 shouldBe aliasName2
                    aliasName1.value shouldBe aliasName2.value
                }
            }
        }

        "alias names should respect length constraints" {
            checkAll(Arb.string(1, 63)) { baseString ->
                val validAlias = "a" + baseString.filter { it.isLetterOrDigit() || it in "-_" }
                val result = AliasName.create(validAlias)

                if (validAlias.length in 2..64) {
                    result.isRight() shouldBe true
                } else {
                    result.isLeft() shouldBe true
                }
            }
        }

        "alias names should start with lowercase letter" {
            checkAll(Arb.char('a'..'z'), validAliasContentArb()) { firstChar, content ->
                val aliasString = firstChar + content
                val result = AliasName.create(aliasString)

                if (aliasString.length in 2..64) {
                    result.isRight() shouldBe true
                    result.getOrNull()!!.value.first().isLowerCase() shouldBe true
                }
            }
        }

        "alias names can contain valid characters only" {
            checkAll(Arb.list(validAliasCharArb(), 1..63)) { charList ->
                val aliasString = 'a' + charList.joinToString("")
                val result = AliasName.create(aliasString)

                if (aliasString.length in 2..64 && aliasString.all { it.isLowerCase() || it.isDigit() || it in "-_" }) {
                    result.isRight() shouldBe true
                }
            }
        }

        "pattern validation property" {
            checkAll(validAliasArb()) { aliasString ->
                val aliasName = AliasName.create(aliasString).getOrNull()!!

                // All valid alias names should match the expected pattern
                aliasName.value shouldBe aliasName.value.lowercase()
                aliasName.value.matches(Regex("[a-z][a-z0-9-_]{1,63}")) shouldBe true
                aliasName.value.length in 2..64 shouldBe true
                aliasName.value.first().isLetter() shouldBe true
                aliasName.value.first().isLowerCase() shouldBe true
            }
        }
    })

// Test helpers
private fun validAliasArb(): Arb<String> = Arb.bind(
    Arb.char('a'..'z'), // First character must be lowercase letter
    validAliasContentArb(), // Rest can be letters, digits, hyphens, underscores
) { first, content ->
    (first + content).take(64) // Ensure max length
}

private fun validAliasContentArb(): Arb<String> = Arb.string(1, 63, validAliasCharArb())

private fun validAliasCharArb(): Arb<Char> = Arb.choice(
    Arb.char('a'..'z'),
    Arb.char('0'..'9'),
    Arb.constant('-'),
    Arb.constant('_'),
)

private fun mixedCaseValidAliasArb(): Arb<String> = Arb.bind(
    Arb.char('a'..'z'), // Start with lowercase
    Arb.string(1, 63, mixedCaseAliasCharArb()),
) { first, content ->
    (first + content).take(64)
}

private fun mixedCaseAliasCharArb(): Arb<Char> = Arb.choice(
    Arb.char('a'..'z'),
    Arb.char('A'..'Z'), // Include uppercase for testing normalization
    Arb.char('0'..'9'),
    Arb.constant('-'),
    Arb.constant('_'),
)

private fun invalidAliasArb(): Arb<String> = Arb.choice(
    Arb.string(1, 1), // Too short
    Arb.string(65, 100), // Too long
    Arb.string(2, 64).map { "1" + it.substring(1) }, // Starts with digit
    Arb.string(2, 64).map { "-" + it.substring(1) }, // Starts with hyphen
    Arb.string(2, 64).map { "_" + it.substring(1) }, // Starts with underscore
    Arb.string(2, 64).map { it.replace(Regex("[a-zA-Z0-9-_]"), "!") }, // Invalid characters
    Arb.string(2, 64).map { "A" + it.substring(1) }, // Starts with uppercase
)
