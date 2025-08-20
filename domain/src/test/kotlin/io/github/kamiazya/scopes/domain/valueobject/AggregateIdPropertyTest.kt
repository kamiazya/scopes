package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.domain.error.AggregateIdError
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.datetime.Clock
import com.github.guepardoapps.kulid.ULID

class AggregateIdPropertyTest : StringSpec({

    "created aggregate IDs should always have valid URI format" {
        checkAll(validAggregateTypeArb(), validIdArb()) { type, id ->
            val result = AggregateId.create(type, id)
            result.isRight() shouldBe true
            result.fold(
                { throw AssertionError("Expected Right but got Left: $it") },
                { aggregateId ->
                    aggregateId.value shouldMatch "^gid://scopes/$type/$id$"
                    aggregateId.aggregateType shouldBe type
                    aggregateId.id shouldBe id
                }
            )
        }
    }

    "aggregate ID string representation should equal its value" {
        checkAll(validAggregateTypeArb(), validIdArb()) { type, id ->
            val result = AggregateId.create(type, id)
            result.isRight() shouldBe true
            result.fold(
                { throw AssertionError("Expected Right but got Left") },
                { aggregateId ->
                    aggregateId.toString() shouldBe aggregateId.value
                }
            )
        }
    }

    "parsed aggregate IDs should maintain all properties" {
        checkAll(validAggregateTypeArb(), validIdArb()) { type, id ->
            val created = AggregateId.create(type, id)
            created.isRight() shouldBe true
            created.fold(
                { throw AssertionError("Expected Right but got Left") },
                { aggregateId ->
                    val parsed = AggregateId.parse(aggregateId.value)
                    parsed.isRight() shouldBe true
                    parsed.fold(
                        { throw AssertionError("Expected Right but got Left on parse: $it") },
                        { parsedAggregateId ->
                            parsedAggregateId shouldBe aggregateId
                            parsedAggregateId.aggregateType shouldBe type
                            parsedAggregateId.id shouldBe id
                        }
                    )
                }
            )
        }
    }

    "empty or blank types should return error" {
        checkAll(Arb.of("", " ", "  ", "\t", "\n", "   \t   "), validIdArb()) { blank, id ->
            val result = AggregateId.create(blank, id)
            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    error shouldBe AggregateIdError.EmptyValue(
                        occurredAt = error.occurredAt,
                        field = "type"
                    )
                },
                { throw AssertionError("Expected Left but got Right") }
            )
        }
    }

    "empty or blank IDs should return error" {
        checkAll(validAggregateTypeArb(), Arb.of("", " ", "  ", "\t", "\n", "   \t   ")) { type, blank ->
            val result = AggregateId.create(type, blank)
            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    error shouldBe AggregateIdError.EmptyValue(
                        occurredAt = error.occurredAt,
                        field = "id"
                    )
                },
                { throw AssertionError("Expected Left but got Right") }
            )
        }
    }

    "invalid aggregate types should return error" {
        checkAll(invalidAggregateTypeArb(), validIdArb()) { invalidType, id ->
            val result = AggregateId.create(invalidType, id)
            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    when (error) {
                        is AggregateIdError.InvalidType -> {
                            error.attemptedType shouldBe invalidType
                            error.validTypes shouldBe setOf("Scope", "ScopeAlias", "ContextView")
                        }
                        else -> throw AssertionError("Expected InvalidType but got $error")
                    }
                },
                { throw AssertionError("Expected Left but got Right") }
            )
        }
    }

    "IDs with invalid format should return error" {
        checkAll(validAggregateTypeArb(), invalidIdArb()) { type, invalidId ->
            val result = AggregateId.create(type, invalidId)
            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    when (error) {
                        is AggregateIdError.InvalidIdFormat -> {
                            error.attemptedId shouldBe invalidId
                        }
                        else -> throw AssertionError("Expected InvalidIdFormat but got $error")
                    }
                },
                { throw AssertionError("Expected Left but got Right") }
            )
        }
    }

    "parsing empty or blank URIs should return error" {
        checkAll(Arb.of("", " ", "  ", "\t", "\n", "   \t   ")) { blank ->
            val result = AggregateId.parse(blank)
            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    error shouldBe AggregateIdError.EmptyValue(
                        occurredAt = error.occurredAt,
                        field = "uri"
                    )
                },
                { throw AssertionError("Expected Left but got Right") }
            )
        }
    }

    "parsing URIs with wrong schema should return error" {
        checkAll(
            Arb.of("evt://scopes/Scope/01HX3BQXYZ", 
                   "http://scopes/Scope/01HX3BQXYZ",
                   "gid://wrong/Scope/01HX3BQXYZ")
        ) { wrongUri ->
            val result = AggregateId.parse(wrongUri)
            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    when (error) {
                        is AggregateIdError.InvalidUriFormat -> {
                            error.attemptedUri shouldBe wrongUri
                        }
                        else -> throw AssertionError("Expected InvalidUriFormat but got $error")
                    }
                },
                { throw AssertionError("Expected Left but got Right") }
            )
        }
    }

    "parsing URIs with invalid structure should return error" {
        checkAll(
            Arb.of(
                "gid://scopes/Scope",  // Missing ID
                "gid://scopes/Scope/01HX3BQXYZ/extra",  // Extra parts
                "gid://scopes/scope/01HX3BQXYZ",  // lowercase type
                "gid://scopes/Scope-Alias/01HX3BQXYZ",  // hyphen in type
                "gid://scopes/Scope/invalid-id"  // invalid ID format
            )
        ) { invalidUri ->
            val result = AggregateId.parse(invalidUri)
            result.isLeft() shouldBe true
        }
    }

    "creating aggregate ID from class should use simple name" {
        class TestAggregate
        val id = ULID().toString()
        val result = AggregateId.create(TestAggregate::class, id)
        
        // Since TestAggregate is not in the valid types, it should fail
        result.isLeft() shouldBe true
        result.fold(
            { error ->
                when (error) {
                    is AggregateIdError.InvalidType -> {
                        error.attemptedType shouldBe "TestAggregate"
                    }
                    else -> throw AssertionError("Expected InvalidType but got $error")
                }
            },
            { throw AssertionError("Expected Left but got Right") }
        )
    }

    "aggregate ID equality should be value-based" {
        val uri = "gid://scopes/Scope/01HX3BQXYZ"
        val result1 = AggregateId.parse(uri)
        val result2 = AggregateId.parse(uri)
        
        result1.isRight() shouldBe true
        result2.isRight() shouldBe true
        
        result1.fold(
            { throw AssertionError("Expected Right but got Left") },
            { id1 ->
                result2.fold(
                    { throw AssertionError("Expected Right but got Left") },
                    { id2 ->
                        (id1 == id2) shouldBe true
                        id1.value shouldBe id2.value
                        id1.aggregateType shouldBe id2.aggregateType
                        id1.id shouldBe id2.id
                    }
                )
            }
        )
    }

    "different aggregate IDs should not be equal" {
        checkAll(validAggregateTypeArb()) { type ->
            val result1 = AggregateId.create(type, ULID().toString())
            val result2 = AggregateId.create(type, ULID().toString())
            
            result1.isRight() shouldBe true
            result2.isRight() shouldBe true
            
            result1.fold(
                { throw AssertionError("Expected Right but got Left") },
                { id1 ->
                    result2.fold(
                        { throw AssertionError("Expected Right but got Left") },
                        { id2 ->
                            id1 shouldNotBe id2
                            id1.id shouldNotBe id2.id
                        }
                    )
                }
            )
        }
    }

    "extracting components from aggregate ID should be consistent" {
        checkAll(validAggregateTypeArb(), validIdArb()) { type, id ->
            val uri = "gid://scopes/$type/$id"
            val result = AggregateId.parse(uri)
            
            result.isRight() shouldBe true
            result.fold(
                { throw AssertionError("Expected Right but got Left") },
                { aggregateId ->
                    aggregateId.aggregateType shouldBe type
                    aggregateId.id shouldBe id
                    aggregateId.value shouldBe uri
                }
            )
        }
    }

    "all valid aggregate types should be accepted" {
        val validTypes = listOf("Scope", "ScopeAlias", "ContextView")
        checkAll(Arb.of(validTypes), validIdArb()) { type, id ->
            val result = AggregateId.create(type, id)
            result.isRight() shouldBe true
        }
    }

    "aggregate ID creation should be idempotent" {
        checkAll(validAggregateTypeArb(), validIdArb()) { type, id ->
            val result1 = AggregateId.create(type, id)
            val result2 = AggregateId.create(type, id)
            
            result1.isRight() shouldBe true
            result2.isRight() shouldBe true
            
            result1.fold(
                { throw AssertionError("Expected Right but got Left") },
                { id1 ->
                    result2.fold(
                        { throw AssertionError("Expected Right but got Left") },
                        { id2 ->
                            id1 shouldBe id2
                            id1.value shouldBe id2.value
                        }
                    )
                }
            )
        }
    }
})

// Custom Arbitrary generators
private fun validAggregateTypeArb(): Arb<String> = Arb.of("Scope", "ScopeAlias", "ContextView")

private fun invalidAggregateTypeArb(): Arb<String> = Arb.choice(
    // Not in the valid list
    Arb.of("User", "Project", "Task", "Comment", "Invalid"),
    // Wrong casing
    Arb.of("scope", "SCOPE", "scopeAlias", "contextview"),
    // Contains special characters
    Arb.string(5..20).map { base ->
        val specialChars = listOf("-", "_", "!", "@", "#", "$")
        val insertPos = (0..base.length).random()
        base.substring(0, insertPos) + specialChars.random() + base.substring(insertPos)
    }
).filter { it.isNotBlank() }

private fun validIdArb(): Arb<String> = arbitrary {
    ULID().toString()
}

private fun invalidIdArb(): Arb<String> = Arb.choice(
    // Contains lowercase letters
    Arb.string(26..26).map { it.lowercase() },
    // Contains special characters
    Arb.string(20..30).map { base ->
        val specialChars = listOf("-", "_", "!", "@", "#", "$", " ")
        val insertPos = (0..base.length).random()
        base.substring(0, insertPos) + specialChars.random() + base.substring(insertPos)
    },
    // Wrong length
    Arb.choice(
        Arb.string(1..25),  // Too short
        Arb.string(27..50)  // Too long
    )
).filter { it.isNotBlank() }