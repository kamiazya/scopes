package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.domain.error.EventIdError
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.checkAll
import kotlinx.datetime.Clock
import com.github.guepardoapps.kulid.ULID

class EventIdPropertyTest : StringSpec({

    "created event IDs should always have valid URI format" {
        checkAll(validEventTypeArb()) { eventType ->
            val result = EventId.create(eventType)
            result.isRight() shouldBe true
            result.fold(
                { throw AssertionError("Expected Right but got Left: $it") },
                { eventId ->
                    eventId.value shouldMatch "^evt://scopes/$eventType/[0-9A-Z]{26}$"
                    eventId.eventType shouldBe eventType
                    ULID.isValid(eventId.ulid) shouldBe true
                }
            )
        }
    }

    "event IDs created with the same type should be unique" {
        checkAll(validEventTypeArb()) { eventType ->
            val ids = (1..100).mapNotNull {
                EventId.create(eventType).fold({ null }, { it })
            }
            ids.distinct().size shouldBe ids.size
        }
    }

    "event ID string representation should equal its value" {
        checkAll(validEventTypeArb()) { eventType ->
            val result = EventId.create(eventType)
            result.isRight() shouldBe true
            result.fold(
                { throw AssertionError("Expected Right but got Left") },
                { eventId ->
                    eventId.toString() shouldBe eventId.value
                }
            )
        }
    }

    "parsed event IDs should maintain all properties" {
        checkAll(validEventTypeArb()) { eventType ->
            val created = EventId.create(eventType)
            created.isRight() shouldBe true
            created.fold(
                { throw AssertionError("Expected Right but got Left") },
                { eventId ->
                    val parsed = EventId.parse(eventId.value)
                    parsed.isRight() shouldBe true
                    parsed.fold(
                        { throw AssertionError("Expected Right but got Left") },
                        { parsedEventId ->
                            parsedEventId shouldBe eventId
                            parsedEventId.eventType shouldBe eventType
                            parsedEventId.ulid shouldBe eventId.ulid
                        }
                    )
                }
            )
        }
    }

    "empty or blank event types should return error" {
        checkAll(Arb.of("", " ", "  ", "\t", "\n", "   \t   ")) { blank ->
            val result = EventId.create(blank)
            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    error shouldBe EventIdError.EmptyValue(
                        occurredAt = error.occurredAt,
                        field = "eventType"
                    )
                },
                { throw AssertionError("Expected Left but got Right") }
            )
        }
    }

    "invalid event type formats should return error" {
        checkAll(invalidEventTypeArb()) { invalidType ->
            val result = EventId.create(invalidType)
            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    when (error) {
                        is EventIdError.InvalidEventType -> {
                            error.attemptedType shouldBe invalidType
                        }
                        else -> throw AssertionError("Expected InvalidEventType but got $error")
                    }
                },
                { throw AssertionError("Expected Left but got Right") }
            )
        }
    }

    "parsing empty or blank URIs should return error" {
        checkAll(Arb.of("", " ", "  ", "\t", "\n", "   \t   ")) { blank ->
            val result = EventId.parse(blank)
            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    error shouldBe EventIdError.EmptyValue(
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
            Arb.of("gid://scopes/ScopeCreated/01HX3BQXYZ",
                "http://scopes/ScopeCreated/01HX3BQXYZ",
                "evt://wrong/ScopeCreated/01HX3BQXYZ")
        ) { wrongUri ->
            val result = EventId.parse(wrongUri)
            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    when (error) {
                        is EventIdError.InvalidUriFormat -> {
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
                "evt://scopes/ScopeCreated",  // Missing ULID
                "evt://scopes/ScopeCreated/01HX3BQXYZ/extra",  // Extra parts
                "evt://scopes/scopeCreated/01HX3BQXYZ",  // lowercase event type
                "evt://scopes/Scope-Created/01HX3BQXYZ",  // hyphen in event type
                "evt://scopes/ScopeCreated/invalid-ulid"  // invalid ULID
            )
        ) { invalidUri ->
            val result = EventId.parse(invalidUri)
            result.isLeft() shouldBe true
        }
    }

    "event IDs should be lexicographically sortable by time" {
        val eventIds = mutableListOf<EventId>()
        val eventType = "TestEvent"
        repeat(10) {
            EventId.create(eventType).fold(
                { throw AssertionError("Failed to create EventId") },
                { eventIds.add(it) }
            )
            Thread.sleep(2) // Small delay to ensure different timestamps
        }
        val sortedIds = eventIds.sortedBy { it.ulid }
        sortedIds shouldBe eventIds
    }

    "creating event ID from class should use simple name" {
        class TestEvent
        val result = EventId.create(TestEvent::class)
        result.isRight() shouldBe true
        result.fold(
            { throw AssertionError("Expected Right but got Left") },
            { eventId ->
                eventId.eventType shouldBe "TestEvent"
            }
        )
    }

    "event ID equality should be value-based" {
        val ulid = ULID.random()
        val uri = "evt://scopes/ScopeCreated/$ulid"
        val result1 = EventId.parse(uri)
        val result2 = EventId.parse(uri)

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
                        id1.eventType shouldBe id2.eventType
                        id1.ulid shouldBe id2.ulid
                    }
                )
            }
        )
    }

    "different event IDs should not be equal" {
        val result1 = EventId.create("EventA")
        val result2 = EventId.create("EventB")

        result1.isRight() shouldBe true
        result2.isRight() shouldBe true

        result1.fold(
            { throw AssertionError("Expected Right but got Left") },
            { id1 ->
                result2.fold(
                    { throw AssertionError("Expected Right but got Left") },
                    { id2 ->
                        id1 shouldNotBe id2
                        id1.eventType shouldNotBe id2.eventType
                    }
                )
            }
        )
    }

    "extracting components from event ID should be consistent" {
        checkAll(validEventTypeArb(), validUlidArb()) { eventType, ulid ->
            val uri = "evt://scopes/$eventType/$ulid"
            val result = EventId.parse(uri)

            result.isRight() shouldBe true
            result.fold(
                { throw AssertionError("Expected Right but got Left") },
                { eventId ->
                    eventId.eventType shouldBe eventType
                    eventId.ulid shouldBe ulid
                    eventId.value shouldBe uri
                }
            )
        }
    }
})

// Custom Arbitrary generators
private fun validEventTypeArb(): Arb<String> = Arb.stringPattern("[A-Z][a-zA-Z]{4,20}")

private fun invalidEventTypeArb(): Arb<String> = Arb.choice(
    // lowercase start
    Arb.string(5..20).map { it.lowercase() },
    // contains special characters
    Arb.string(5..20).map { base ->
        val specialChars = listOf("-", "_", "!", "@", "#", "$", "%", "^", "&", "*")
        val insertPos = (0..base.length).random()
        base.substring(0, insertPos) + specialChars.random() + base.substring(insertPos)
    },
    // contains numbers
    Arb.string(5..20).map { base ->
        val insertPos = (0..base.length).random()
        base.substring(0, insertPos) + (0..9).random() + base.substring(insertPos)
    },
    // contains spaces
    Arb.string(5..20).map { base ->
        val insertPos = (1 until base.length).random()
        base.substring(0, insertPos) + " " + base.substring(insertPos)
    }
).filter { it.isNotBlank() }

private fun validUlidArb(): Arb<String> = arbitrary {
    ULID.random()
}
