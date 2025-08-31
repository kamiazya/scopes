package io.github.kamiazya.scopes.eventstore.infrastructure.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.eventstore.application.port.EventSerializer
import io.github.kamiazya.scopes.eventstore.domain.entity.PersistedEventRecord
import io.github.kamiazya.scopes.eventstore.domain.error.EventStoreError
import io.github.kamiazya.scopes.eventstore.domain.valueobject.EventMetadata
import io.github.kamiazya.scopes.eventstore.domain.valueobject.EventType
import io.github.kamiazya.scopes.eventstore.infrastructure.sqldelight.SqlDelightDatabaseProvider
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// Test domain event for testing
data class TestEvent(
    override val eventId: EventId,
    override val aggregateId: AggregateId,
    override val aggregateVersion: AggregateVersion,
    override val occurredAt: Instant,
    val testData: String,
) : DomainEvent

// Mock serializer for testing
class MockEventSerializer : EventSerializer {
    private val events = mutableMapOf<String, DomainEvent>()
    var serializeError: EventStoreError? = null
    var deserializeError: EventStoreError? = null

    override fun serialize(event: DomainEvent): Either<EventStoreError.InvalidEventError, String> {
        serializeError?.let { 
            return if (it is EventStoreError.InvalidEventError) {
                it.left()
            } else {
                EventStoreError.InvalidEventError(
                    eventType = event::class.simpleName ?: "Unknown",
                    validationErrors = listOf(
                        EventStoreError.ValidationIssue(
                            field = "serialization",
                            rule = EventStoreError.ValidationRule.INVALID_TYPE,
                            actualValue = "Serialization error"
                        )
                    )
                ).left()
            }
        }

        val key = "${event.eventId.value}:${event::class.simpleName}"
        events[key] = event
        return key.right()
    }

    override fun deserialize(eventType: String, eventData: String): Either<EventStoreError.InvalidEventError, DomainEvent> {
        deserializeError?.let { 
            return if (it is EventStoreError.InvalidEventError) {
                it.left()
            } else {
                EventStoreError.InvalidEventError(
                    eventType = eventType,
                    validationErrors = listOf(
                        EventStoreError.ValidationIssue(
                            field = "deserialization",
                            rule = EventStoreError.ValidationRule.INVALID_TYPE,
                            actualValue = "Deserialization error"
                        )
                    )
                ).left()
            }
        }

        return events[eventData]?.right() ?: EventStoreError.InvalidEventError(
            eventType = eventType,
            validationErrors = listOf(
                EventStoreError.ValidationIssue(
                    field = "eventData",
                    rule = EventStoreError.ValidationRule.INVALID_TYPE,
                    actualValue = "Event not found",
                ),
            ),
        ).left()
    }
}

class SqlDelightEventRepositoryTest :
    DescribeSpec({

        describe("SqlDelightEventRepository") {
            lateinit var repository: SqlDelightEventRepository
            lateinit var serializer: MockEventSerializer
            lateinit var database: io.github.kamiazya.scopes.eventstore.db.EventStoreDatabase
            lateinit var driver: app.cash.sqldelight.db.SqlDriver

            beforeEach {
                serializer = MockEventSerializer()
                driver = app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver(app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.IN_MEMORY)
                io.github.kamiazya.scopes.eventstore.db.EventStoreDatabase.Schema.create(driver)
                database = io.github.kamiazya.scopes.eventstore.db.EventStoreDatabase(driver)
                repository = SqlDelightEventRepository(database.eventQueries, serializer)
            }

            afterEach {
                driver.close()
            }

            describe("store") {
                it("should store a domain event") {
                    // Given
                    val event = TestEvent(
                        eventId = EventId.generate(),
                        aggregateId = AggregateId.generate(),
                        aggregateVersion = AggregateVersion.initial(),
                        occurredAt = Clock.System.now(),
                        testData = "test data",
                    )

                    // When
                    val result = runTest { repository.store(event) }

                    // Then
                    result.isRight() shouldBe true
                    val storedEvent = result.getOrNull()
                    storedEvent shouldNotBe null
                    storedEvent?.metadata?.eventId shouldBe event.eventId
                    storedEvent?.metadata?.aggregateId shouldBe event.aggregateId
                    storedEvent?.metadata?.aggregateVersion shouldBe event.aggregateVersion
                    storedEvent?.metadata?.sequenceNumber shouldNotBe null
                    storedEvent?.event shouldBe event
                }

                it("should assign sequential sequence numbers") {
                    // Given
                    val aggregateId = AggregateId.generate()
                    val events = listOf(
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = aggregateId,
                            aggregateVersion = AggregateVersion.initial(),
                            occurredAt = Clock.System.now(),
                            testData = "event 1",
                        ),
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = aggregateId,
                            aggregateVersion = AggregateVersion.fromUnsafe(2),
                            occurredAt = Clock.System.now(),
                            testData = "event 2",
                        ),
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = aggregateId,
                            aggregateVersion = AggregateVersion.fromUnsafe(3),
                            occurredAt = Clock.System.now(),
                            testData = "event 3",
                        ),
                    )

                    // When
                    val results = runTest {
                        events.map { repository.store(it) }
                    }

                    // Then
                    val sequenceNumbers = results.mapNotNull { it.getOrNull()?.metadata?.sequenceNumber }
                    sequenceNumbers shouldBe listOf(1L, 2L, 3L)
                }

                it("should handle serialization errors") {
                    // Given
                    val event = TestEvent(
                        eventId = EventId.generate(),
                        aggregateId = AggregateId.generate(),
                        aggregateVersion = AggregateVersion.initial(),
                        occurredAt = Clock.System.now(),
                        testData = "test data",
                    )
                    serializer.serializeError = EventStoreError.InvalidEventError(
                        eventType = "TestEvent",
                        validationErrors = listOf(
                            EventStoreError.ValidationIssue(
                                field = "test",
                                rule = EventStoreError.ValidationRule.INVALID_TYPE,
                                actualValue = "error",
                            ),
                        ),
                    )

                    // When
                    val result = runTest { repository.store(event) }

                    // Then
                    result.isLeft() shouldBe true
                    result.leftOrNull().shouldBeInstanceOf<EventStoreError.InvalidEventError>()
                }

                it("should handle storage errors") {
                    // Given
                    val event = TestEvent(
                        eventId = EventId.generate(),
                        aggregateId = AggregateId.generate(),
                        aggregateVersion = AggregateVersion.initial(),
                        occurredAt = Clock.System.now(),
                        testData = "test data",
                    )

                    // Store the same event twice to trigger a unique constraint violation
                    runTest { repository.store(event) }

                    // When
                    val result = runTest { repository.store(event) }

                    // Then
                    result.isLeft() shouldBe true
                    result.leftOrNull().shouldBeInstanceOf<EventStoreError.StorageError>()
                }
            }

            describe("getEventsSince") {
                it("should retrieve events since a given timestamp") {
                    // Given
                    val now = Clock.System.now()
                    val past = now.minus(1.hours)
                    val future = now.plus(1.hours)

                    val pastEvent = TestEvent(
                        eventId = EventId.generate(),
                        aggregateId = AggregateId.generate(),
                        aggregateVersion = AggregateVersion.initial(),
                        occurredAt = past,
                        testData = "past event",
                    )

                    val recentEvent = TestEvent(
                        eventId = EventId.generate(),
                        aggregateId = AggregateId.generate(),
                        aggregateVersion = AggregateVersion.initial(),
                        occurredAt = future,
                        testData = "recent event",
                    )

                    runTest {
                        repository.store(pastEvent)
                        repository.store(recentEvent)
                    }

                    // When
                    val result = runTest { repository.getEventsSince(now) }

                    // Then
                    result.isRight() shouldBe true
                    val events = result.getOrNull()
                    events?.shouldHaveSize(1)
                    (events?.first()?.event as? TestEvent)?.testData shouldBe "recent event"
                }

                it("should respect the limit parameter") {
                    // Given
                    val baseTime = Clock.System.now()
                    val events = (1..10).map { i ->
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = AggregateId.generate(),
                            aggregateVersion = AggregateVersion.initial(),
                            occurredAt = baseTime.plus(i.minutes),
                            testData = "event $i",
                        )
                    }

                    runTest {
                        events.forEach { repository.store(it) }
                    }

                    // When
                    val result = runTest { repository.getEventsSince(baseTime, limit = 3) }

                    // Then
                    result.isRight() shouldBe true
                    result.getOrNull()?.shouldHaveSize(3)
                }

                it("should handle deserialization errors gracefully by skipping failed events") {
                    // Given
                    val event1 = TestEvent(
                        eventId = EventId.generate(),
                        aggregateId = AggregateId.generate(),
                        aggregateVersion = AggregateVersion.initial(),
                        occurredAt = Clock.System.now(),
                        testData = "event 1",
                    )

                    val event2 = TestEvent(
                        eventId = EventId.generate(),
                        aggregateId = AggregateId.generate(),
                        aggregateVersion = AggregateVersion.initial(),
                        occurredAt = Clock.System.now(),
                        testData = "event 2",
                    )

                    runTest {
                        repository.store(event1)
                        repository.store(event2)
                    }

                    // Set deserialize error to affect one event
                    serializer.deserializeError = EventStoreError.InvalidEventError(
                        eventType = "TestEvent",
                        validationErrors = listOf(
                            EventStoreError.ValidationIssue(
                                field = "test",
                                rule = EventStoreError.ValidationRule.INVALID_TYPE,
                                actualValue = "error",
                            ),
                        ),
                    )

                    // When
                    val result = runTest { repository.getEventsSince(Instant.DISTANT_PAST) }

                    // Then
                    result.isRight() shouldBe true
                    result.getOrNull() shouldBe emptyList() // All events failed to deserialize
                }
            }

            describe("getEventsByAggregate") {
                it("should retrieve all events for an aggregate") {
                    // Given
                    val aggregateId = AggregateId.generate()
                    val otherAggregateId = AggregateId.generate()

                    val events = listOf(
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = aggregateId,
                            aggregateVersion = AggregateVersion.initial(),
                            occurredAt = Clock.System.now(),
                            testData = "aggregate event 1",
                        ),
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = otherAggregateId,
                            aggregateVersion = AggregateVersion.initial(),
                            occurredAt = Clock.System.now(),
                            testData = "other aggregate event",
                        ),
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = aggregateId,
                            aggregateVersion = AggregateVersion.fromUnsafe(2),
                            occurredAt = Clock.System.now(),
                            testData = "aggregate event 2",
                        ),
                    )

                    runTest {
                        events.forEach { repository.store(it) }
                    }

                    // When
                    val result = runTest { repository.getEventsByAggregate(aggregateId) }

                    // Then
                    result.isRight() shouldBe true
                    val aggregateEvents = result.getOrNull()
                    aggregateEvents?.shouldHaveSize(2)
                    aggregateEvents?.all { it.metadata.aggregateId == aggregateId } shouldBe true
                }

                it("should retrieve events for an aggregate since a timestamp") {
                    // Given
                    val aggregateId = AggregateId.generate()
                    val now = Clock.System.now()
                    val past = now.minus(1.hours)

                    val oldEvent = TestEvent(
                        eventId = EventId.generate(),
                        aggregateId = aggregateId,
                        aggregateVersion = AggregateVersion.initial(),
                        occurredAt = past,
                        testData = "old event",
                    )

                    val recentEvent = TestEvent(
                        eventId = EventId.generate(),
                        aggregateId = aggregateId,
                        aggregateVersion = AggregateVersion.fromUnsafe(2),
                        occurredAt = now.plus(1.minutes),
                        testData = "recent event",
                    )

                    runTest {
                        repository.store(oldEvent)
                        Thread.sleep(100) // Ensure different stored_at timestamps
                        repository.store(recentEvent)
                    }

                    // When
                    val result = runTest { repository.getEventsByAggregate(aggregateId, since = now) }

                    // Then
                    result.isRight() shouldBe true
                    val events = result.getOrNull()
                    events?.shouldHaveSize(1)
                    (events?.first()?.event as? TestEvent)?.testData shouldBe "recent event"
                }

                it("should respect limit when retrieving aggregate events") {
                    // Given
                    val aggregateId = AggregateId.generate()
                    val events = (1..5).map { i ->
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = aggregateId,
                            aggregateVersion = AggregateVersion.fromUnsafe(i.toLong()),
                            occurredAt = Clock.System.now(),
                            testData = "event $i",
                        )
                    }

                    runTest {
                        events.forEach { repository.store(it) }
                    }

                    // When
                    val result = runTest { repository.getEventsByAggregate(aggregateId, limit = 2) }

                    // Then
                    result.isRight() shouldBe true
                    result.getOrNull()?.shouldHaveSize(2)
                }

                it("should return empty list for aggregate with no events") {
                    // Given
                    val aggregateId = AggregateId.generate()

                    // When
                    val result = runTest { repository.getEventsByAggregate(aggregateId) }

                    // Then
                    result shouldBe emptyList<PersistedEventRecord>().right()
                }
            }

            describe("streamEvents") {
                it("should stream all events") {
                    // Given
                    val events = listOf(
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = AggregateId.generate(),
                            aggregateVersion = AggregateVersion.initial(),
                            occurredAt = Clock.System.now(),
                            testData = "stream event 1",
                        ),
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = AggregateId.generate(),
                            aggregateVersion = AggregateVersion.initial(),
                            occurredAt = Clock.System.now(),
                            testData = "stream event 2",
                        ),
                    )

                    runTest {
                        events.forEach { repository.store(it) }
                    }

                    // When
                    val streamedEvents = runTest {
                        repository.streamEvents().toList()
                    }

                    // Then
                    streamedEvents shouldHaveSize 2
                    streamedEvents.map { (it.event as TestEvent).testData } shouldContainExactly listOf("stream event 1", "stream event 2")
                }

                it("should handle empty event store") {
                    // When
                    val streamedEvents = runTest {
                        repository.streamEvents().toList()
                    }

                    // Then
                    streamedEvents shouldBe emptyList()
                }
            }

            describe("event ordering") {
                it("should maintain event order by sequence number") {
                    // Given
                    val aggregateId = AggregateId.generate()
                    val events = (1..5).map { i ->
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = aggregateId,
                            aggregateVersion = AggregateVersion.fromUnsafe(i.toLong()),
                            occurredAt = Clock.System.now().plus(i.seconds),
                            testData = "event $i",
                        )
                    }

                    // Store events in random order
                    runTest {
                        listOf(events[2], events[0], events[4], events[1], events[3]).forEach {
                            repository.store(it)
                        }
                    }

                    // When
                    val result = runTest { repository.getEventsByAggregate(aggregateId) }

                    // Then
                    result.isRight() shouldBe true
                    val retrievedEvents = result.getOrNull()
                    retrievedEvents?.shouldHaveSize(5)

                    // Events should be ordered by sequence number (which is insertion order)
                    val sequenceNumbers = retrievedEvents?.map { it.metadata.sequenceNumber }
                    sequenceNumbers?.sorted() shouldBe sequenceNumbers
                }
            }

            describe("error handling") {
                it("should handle persistence errors") {
                    // Given
                    val aggregateId = AggregateId.generate()
                    driver.close()

                    // When
                    val result = runTest { repository.getEventsByAggregate(aggregateId) }

                    // Then
                    result.isLeft() shouldBe true
                    result.leftOrNull().shouldBeInstanceOf<EventStoreError.PersistenceError>()
                }
            }

            describe("event metadata") {
                it("should correctly persist and retrieve event metadata") {
                    // Given
                    val event = TestEvent(
                        eventId = EventId.generate(),
                        aggregateId = AggregateId.generate(),
                        aggregateVersion = AggregateVersion.fromUnsafe(42),
                        occurredAt = Clock.System.now(),
                        testData = "metadata test",
                    )

                    // When
                    val storeResult = runTest { repository.store(event) }
                    val retrieveResult = runTest { repository.getEventsSince(Instant.DISTANT_PAST) }

                    // Then
                    storeResult.isRight() shouldBe true
                    retrieveResult.isRight() shouldBe true

                    val retrievedEvent = retrieveResult.getOrNull()?.first()
                    retrievedEvent?.metadata?.eventId shouldBe event.eventId
                    retrievedEvent?.metadata?.aggregateId shouldBe event.aggregateId
                    retrievedEvent?.metadata?.aggregateVersion shouldBe event.aggregateVersion
                    retrievedEvent?.metadata?.eventType shouldBe EventType("TestEvent")
                    retrievedEvent?.metadata?.occurredAt shouldBe event.occurredAt
                    retrievedEvent?.metadata?.storedAt shouldNotBe null
                    retrievedEvent?.metadata?.sequenceNumber shouldBe 1L
                }
            }
        }
    })