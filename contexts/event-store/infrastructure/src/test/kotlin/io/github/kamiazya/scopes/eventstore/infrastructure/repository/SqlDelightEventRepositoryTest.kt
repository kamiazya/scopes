package io.github.kamiazya.scopes.eventstore.infrastructure.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.eventstore.application.port.EventSerializer
import io.github.kamiazya.scopes.eventstore.domain.entity.PersistedEventRecord
import io.github.kamiazya.scopes.eventstore.domain.error.EventStoreError
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
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
                            actualValue = "Serialization error",
                        ),
                    ),
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
                            actualValue = "Deserialization error",
                        ),
                    ),
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
                        testData = "test data",
                    )

                    // When
                    val result = runBlocking { repository.store(event) }

                    // Then
                    result.isRight() shouldBe true
                    val storedEvent = result.getOrNull()!!
                    storedEvent.metadata.eventId shouldBe event.eventId
                    storedEvent.metadata.aggregateId shouldBe event.aggregateId
                    storedEvent.metadata.aggregateVersion shouldBe event.aggregateVersion
                    storedEvent.metadata.sequenceNumber shouldNotBe null
                    storedEvent.event shouldBe event
                }

                it("should assign sequential sequence numbers") {
                    // Given
                    val aggregateId = AggregateId.generate()
                    val events = listOf(
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = aggregateId,
                            aggregateVersion = AggregateVersion.initial(),
                            testData = "event 1",
                        ),
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = aggregateId,
                            aggregateVersion = AggregateVersion.fromUnsafe(2),
                            testData = "event 2",
                        ),
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = aggregateId,
                            aggregateVersion = AggregateVersion.fromUnsafe(3),
                            testData = "event 3",
                        ),
                    )

                    // When
                    val results = runBlocking {
                        events.map { repository.store(it) }
                    }

                    // Then
                    results.forEach { it.isRight() shouldBe true }
                    val sequenceNumbers = results.map { it.getOrNull()!!.metadata.sequenceNumber }
                    sequenceNumbers shouldBe listOf(1L, 2L, 3L)
                }

                it("should handle serialization errors") {
                    // Given
                    val event = TestEvent(
                        eventId = EventId.generate(),
                        aggregateId = AggregateId.generate(),
                        aggregateVersion = AggregateVersion.initial(),
                        testData = "test data",
                    )
                    serializer.serializeError = EventStoreError.InvalidEventError(
                        eventType = "io.github.kamiazya.scopes.eventstore.infrastructure.repository.TestEvent",
                        validationErrors = listOf(
                            EventStoreError.ValidationIssue(
                                field = "test",
                                rule = EventStoreError.ValidationRule.INVALID_TYPE,
                                actualValue = "error",
                            ),
                        ),
                    )

                    // When
                    val result = runBlocking { repository.store(event) }

                    // Then
                    result.isLeft() shouldBe true
                    val error = result.leftOrNull().shouldBeInstanceOf<EventStoreError.InvalidEventError>()
                    error.eventType shouldBe "io.github.kamiazya.scopes.eventstore.infrastructure.repository.TestEvent"
                    error.validationErrors.shouldNotBeEmpty()
                }

                it("should handle storage errors") {
                    // Given
                    val event = TestEvent(
                        eventId = EventId.generate(),
                        aggregateId = AggregateId.generate(),
                        aggregateVersion = AggregateVersion.initial(),
                        testData = "test data",
                    )

                    // Store the same event twice to trigger a unique constraint violation
                    runBlocking { repository.store(event) }

                    // When
                    val result = runBlocking { repository.store(event) }

                    // Then
                    result.isLeft() shouldBe true
                    val error = result.leftOrNull().shouldBeInstanceOf<EventStoreError.StorageError>()
                    error.aggregateId shouldBe event.aggregateId.value
                    // Event type will be the fully qualified name since we're using a mock serializer
                    error.eventType shouldBe (TestEvent::class.qualifiedName ?: "TestEvent")
                    error.storageFailureType shouldNotBe null
                }
            }

            describe("getEventsSince") {
                it("should retrieve events since a given timestamp") {
                    // Given
                    val baseTime = Clock.System.now()

                    // Create first event with a timestamp in the past
                    val firstEvent = TestEvent(
                        eventId = EventId.generate(),
                        aggregateId = AggregateId.generate(),
                        aggregateVersion = AggregateVersion.initial(),
                        occurredAt = baseTime.minus(10.seconds),
                        testData = "first event",
                    )

                    // Store first event
                    val firstStoreResult = runBlocking {
                        repository.store(firstEvent)
                    }
                    firstStoreResult.isRight() shouldBe true
                    val firstStoredEvent = firstStoreResult.getOrNull()!!

                    // Add a small delay to ensure database timestamp precision
                    runBlocking { delay(100) }

                    // Use a timestamp slightly after the first event's stored time
                    // This ensures the query (which uses >=) won't include the first event
                    val cutoffTimestamp = Instant.fromEpochMilliseconds(
                        firstStoredEvent.metadata.storedAt.toEpochMilliseconds() + 1,
                    )

                    // Create second event with a more recent timestamp
                    val secondEvent = TestEvent(
                        eventId = EventId.generate(),
                        aggregateId = AggregateId.generate(),
                        aggregateVersion = AggregateVersion.initial(),
                        occurredAt = baseTime.minus(5.seconds),
                        testData = "second event",
                    )

                    val secondStoreResult = runBlocking {
                        repository.store(secondEvent)
                    }
                    secondStoreResult.isRight() shouldBe true

                    // When - get events since the cutoff timestamp (after first event was stored)
                    val result = runBlocking { repository.getEventsSince(cutoffTimestamp) }

                    // Then
                    result.isRight() shouldBe true
                    val retrievedEvents = result.getOrNull()!!
                    retrievedEvents.shouldHaveSize(1)
                    (retrievedEvents.first().event as TestEvent).testData shouldBe "second event"
                }

                it("should respect the limit parameter") {
                    // Given
                    val events = (1..10).map { i ->
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = AggregateId.generate(),
                            aggregateVersion = AggregateVersion.initial(),
                            testData = "event $i",
                        )
                    }

                    // Store all events and get the timestamp before storing
                    val baseTime = Clock.System.now()
                    runBlocking {
                        events.forEach { repository.store(it) }
                    }

                    // When
                    val result = runBlocking { repository.getEventsSince(baseTime, limit = 3) }

                    // Then
                    result.isRight() shouldBe true
                    val retrievedEvents = result.getOrNull()!!
                    retrievedEvents.shouldHaveSize(3)
                }

                it("should handle deserialization errors gracefully by skipping failed events") {
                    // Given
                    val event1 = TestEvent(
                        eventId = EventId.generate(),
                        aggregateId = AggregateId.generate(),
                        aggregateVersion = AggregateVersion.initial(),
                        testData = "event 1",
                    )

                    val event2 = TestEvent(
                        eventId = EventId.generate(),
                        aggregateId = AggregateId.generate(),
                        aggregateVersion = AggregateVersion.initial(),
                        testData = "event 2",
                    )

                    runBlocking {
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
                    val result = runBlocking { repository.getEventsSince(Instant.DISTANT_PAST) }

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
                            testData = "aggregate event 1",
                        ),
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = otherAggregateId,
                            aggregateVersion = AggregateVersion.initial(),
                            testData = "other aggregate event",
                        ),
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = aggregateId,
                            aggregateVersion = AggregateVersion.fromUnsafe(2),
                            testData = "aggregate event 2",
                        ),
                    )

                    runBlocking {
                        events.forEach { repository.store(it) }
                    }

                    // When
                    val result = runBlocking { repository.getEventsByAggregate(aggregateId) }

                    // Then
                    result.isRight() shouldBe true
                    val aggregateEvents = result.getOrNull()!!
                    aggregateEvents.shouldHaveSize(2)
                    aggregateEvents.all { it.metadata.aggregateId == aggregateId } shouldBe true
                }

                it("should retrieve events for an aggregate since a timestamp") {
                    // Given
                    val aggregateId = AggregateId.generate()
                    val baseTime = Clock.System.now()

                    // Create old event with a timestamp in the past
                    val oldEvent = TestEvent(
                        eventId = EventId.generate(),
                        aggregateId = aggregateId,
                        aggregateVersion = AggregateVersion.initial(),
                        occurredAt = baseTime.minus(10.seconds),
                        testData = "old event",
                    )

                    // Store old event
                    val oldStoreResult = runBlocking {
                        repository.store(oldEvent)
                    }
                    oldStoreResult.isRight() shouldBe true
                    val oldStoredEvent = oldStoreResult.getOrNull()!!

                    // Add a small delay to ensure database timestamp precision
                    runBlocking { delay(100) }

                    // Use a timestamp slightly after the old event's stored time
                    // This ensures the query (which uses >=) won't include the old event
                    val cutoffTimestamp = Instant.fromEpochMilliseconds(
                        oldStoredEvent.metadata.storedAt.toEpochMilliseconds() + 1,
                    )

                    // Create recent event with a more recent timestamp
                    val recentEvent = TestEvent(
                        eventId = EventId.generate(),
                        aggregateId = aggregateId,
                        aggregateVersion = AggregateVersion.fromUnsafe(2),
                        occurredAt = baseTime.minus(5.seconds),
                        testData = "recent event",
                    )

                    val recentStoreResult = runBlocking {
                        repository.store(recentEvent)
                    }
                    recentStoreResult.isRight() shouldBe true

                    // When - get events since the cutoff timestamp (after old event was stored)
                    val result = runBlocking { repository.getEventsByAggregate(aggregateId, since = cutoffTimestamp) }

                    // Then
                    result.isRight() shouldBe true
                    val retrievedEvents = result.getOrNull()!!
                    retrievedEvents.shouldHaveSize(1)
                    (retrievedEvents.first().event as TestEvent).testData shouldBe "recent event"
                }

                it("should respect limit when retrieving aggregate events") {
                    // Given
                    val aggregateId = AggregateId.generate()
                    val events = (1..5).map { i ->
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = aggregateId,
                            aggregateVersion = AggregateVersion.fromUnsafe(i.toLong()),
                            testData = "event $i",
                        )
                    }

                    runBlocking {
                        events.forEach { repository.store(it) }
                    }

                    // When
                    val result = runBlocking { repository.getEventsByAggregate(aggregateId, limit = 2) }

                    // Then
                    result.isRight() shouldBe true
                    val limitedEvents = result.getOrNull()!!
                    limitedEvents.shouldHaveSize(2)
                }

                it("should return empty list for aggregate with no events") {
                    // Given
                    val aggregateId = AggregateId.generate()

                    // When
                    val result = runBlocking { repository.getEventsByAggregate(aggregateId) }

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
                            testData = "stream event 1",
                        ),
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = AggregateId.generate(),
                            aggregateVersion = AggregateVersion.initial(),
                            testData = "stream event 2",
                        ),
                    )

                    runBlocking {
                        events.forEach { repository.store(it) }
                    }

                    // When
                    val streamedEvents = runBlocking {
                        repository.streamEvents().toList()
                    }

                    // Then
                    streamedEvents shouldHaveSize 2
                    streamedEvents.map { (it.event as TestEvent).testData } shouldContainExactly listOf("stream event 1", "stream event 2")
                }

                it("should handle empty event store") {
                    // When
                    val streamedEvents = runBlocking {
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
                    // Use past timestamps to avoid "Event cannot be stored before it occurred" validation error
                    val baseTime = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds() - 60000)
                    val events = (1..5).map { i ->
                        TestEvent(
                            eventId = EventId.generate(),
                            aggregateId = aggregateId,
                            aggregateVersion = AggregateVersion.fromUnsafe(i.toLong()),
                            occurredAt = baseTime.plus(i.seconds),
                            testData = "event $i",
                        )
                    }

                    // Store events in random order
                    runBlocking {
                        listOf(events[2], events[0], events[4], events[1], events[3]).forEach { event ->
                            val storeResult = repository.store(event)
                            storeResult.isRight() shouldBe true // Ensure each event is stored successfully
                        }
                    }

                    // When
                    val result = runBlocking { repository.getEventsByAggregate(aggregateId) }

                    // Then
                    result.isRight() shouldBe true
                    val retrievedEvents = result.getOrNull()!!
                    retrievedEvents.shouldHaveSize(5)

                    // Events should be ordered by aggregate version (not sequence number)
                    // The SQL query orders by aggregate_version ASC
                    val versions = retrievedEvents.map { it.metadata.aggregateVersion!!.value }
                    versions shouldBe listOf(1L, 2L, 3L, 4L, 5L)
                }
            }

            describe("error handling") {
                it("should handle persistence errors") {
                    // Given
                    val aggregateId = AggregateId.generate()
                    driver.close()

                    // When
                    val result = runBlocking { repository.getEventsByAggregate(aggregateId) }

                    // Then
                    result.isLeft() shouldBe true
                    result.leftOrNull().shouldBeInstanceOf<EventStoreError.PersistenceError>()
                }
            }

            describe("event metadata") {
                it("should correctly persist and retrieve event metadata") {
                    // Given
                    // Use millisecond precision for occurredAt since SQLite stores as milliseconds
                    val now = Clock.System.now()
                    val occurredAtMillis = Instant.fromEpochMilliseconds(now.toEpochMilliseconds())
                    val event = TestEvent(
                        eventId = EventId.generate(),
                        aggregateId = AggregateId.generate(),
                        aggregateVersion = AggregateVersion.fromUnsafe(42),
                        occurredAt = occurredAtMillis,
                        testData = "metadata test",
                    )

                    // When
                    val storeResult = runBlocking { repository.store(event) }
                    val retrieveResult = runBlocking { repository.getEventsSince(Instant.DISTANT_PAST) }

                    // Then
                    storeResult.isRight() shouldBe true
                    retrieveResult.isRight() shouldBe true

                    val events = retrieveResult.getOrNull()!!
                    val retrievedEvent = events.first()
                    retrievedEvent.metadata.eventId shouldBe event.eventId
                    retrievedEvent.metadata.aggregateId shouldBe event.aggregateId
                    retrievedEvent.metadata.aggregateVersion shouldBe event.aggregateVersion
                    // EventType now depends on whether @EventTypeId is present or legacy class name is used
                    retrievedEvent.metadata.eventType.value shouldBe "io.github.kamiazya.scopes.eventstore.infrastructure.repository.TestEvent"
                    retrievedEvent.metadata.occurredAt shouldBe event.occurredAt
                    retrievedEvent.metadata.storedAt shouldNotBe null
                    retrievedEvent.metadata.sequenceNumber shouldNotBe null // Sequence number is auto-generated
                }
            }
        }
    })
