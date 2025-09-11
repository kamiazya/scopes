package io.github.kamiazya.scopes.collaborativeversioning.infrastructure.adapters

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.collaborativeversioning.application.error.EventPublishingError
import io.github.kamiazya.scopes.collaborativeversioning.domain.event.ProposalCreated
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreCommandPort
import io.github.kamiazya.scopes.contracts.eventstore.errors.EventStoreContractError
import io.github.kamiazya.scopes.eventstore.application.port.EventSerializer
import io.github.kamiazya.scopes.eventstore.domain.error.EventStoreError
import io.github.kamiazya.scopes.eventstore.domain.model.EventTypeMapping
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

class EventStoreDomainEventPublisherTest :
    DescribeSpec({

        describe("EventStoreDomainEventPublisher") {

            val mockEventStoreCommandPort = mockk<EventStoreCommandPort>()
            val mockEventSerializer = mockk<EventSerializer>()
            val mockEventTypeMapping = mockk<EventTypeMapping>()

            val publisher = EventStoreDomainEventPublisher(
                eventStoreCommandPort = mockEventStoreCommandPort,
                eventSerializer = mockEventSerializer,
                eventTypeMapping = mockEventTypeMapping,
                publishTimeout = 5.seconds,
            )

            beforeEach {
                clearMocks(mockEventStoreCommandPort, mockEventSerializer, mockEventTypeMapping)
            }

            describe("publish") {

                it("should successfully publish a valid event") {
                    // Given
                    val event = createTestProposalCreatedEvent()
                    val serializedData = """{"proposalId":"test-id","title":"Test Proposal"}"""

                    every { mockEventTypeMapping.getTypeId(ProposalCreated::class) } returns "collaborative-versioning.proposal.created.v1"
                    every { mockEventSerializer.serialize(event) } returns serializedData.right()
                    coEvery { mockEventStoreCommandPort.createEvent(any()) } returns Unit.right()

                    // When
                    val result = runBlocking { publisher.publish(event) }

                    // Then
                    result.isRight() shouldBe true

                    // Verify the command was sent with correct data
                    coVerify {
                        mockEventStoreCommandPort.createEvent(
                            withArg { command ->
                                command.aggregateId shouldBe event.aggregateId.toString()
                                command.aggregateVersion shouldBe event.aggregateVersion.value
                                command.eventType shouldBe "collaborative-versioning.proposal.created.v1"
                                command.eventData shouldBe serializedData
                                command.occurredAt shouldBe event.occurredAt
                                command.metadata["eventId"] shouldBe event.eventId.toString()
                            },
                        )
                    }
                }

                it("should return error when event type is not registered") {
                    // Given
                    val event = createTestProposalCreatedEvent()

                    every { mockEventTypeMapping.getTypeId(ProposalCreated::class) } throws
                        IllegalArgumentException("Event type not registered")

                    // When
                    val result = runBlocking { publisher.publish(event) }

                    // Then
                    result.isLeft() shouldBe true
                    result.leftOrNull().shouldBeInstanceOf<EventPublishingError.UnregisteredEventType>()
                }

                it("should return error when serialization fails") {
                    // Given
                    val event = createTestProposalCreatedEvent()
                    val serializationError = EventStoreError.InvalidEventError(
                        eventType = "collaborative-versioning.proposal.created.v1",
                        validationErrors = listOf(
                            EventStoreError.ValidationIssue(
                                field = "data",
                                rule = EventStoreError.ValidationRule.INVALID_FORMAT,
                                actualValue = "Invalid JSON",
                            ),
                        ),
                        occurredAt = event.occurredAt,
                    )

                    every { mockEventTypeMapping.getTypeId(ProposalCreated::class) } returns "collaborative-versioning.proposal.created.v1"
                    every { mockEventSerializer.serialize(event) } returns serializationError.left()

                    // When
                    val result = runBlocking { publisher.publish(event) }

                    // Then
                    result.isLeft() shouldBe true
                    val error = result.leftOrNull().shouldBeInstanceOf<EventPublishingError.SerializationFailed>()
                    error.eventId shouldBe event.eventId
                    error.eventType shouldBe "collaborative-versioning.proposal.created.v1"
                }

                it("should return error when event store command fails") {
                    // Given
                    val event = createTestProposalCreatedEvent()
                    val serializedData = """{"proposalId":"test-id"}"""
                    val storeError = EventStoreContractError.StorageFailed(
                        reason = "Database connection failed",
                    )

                    every { mockEventTypeMapping.getTypeId(ProposalCreated::class) } returns "collaborative-versioning.proposal.created.v1"
                    every { mockEventSerializer.serialize(event) } returns serializedData.right()
                    coEvery { mockEventStoreCommandPort.createEvent(any()) } returns storeError.left()

                    // When
                    val result = runBlocking { publisher.publish(event) }

                    // Then
                    result.isLeft() shouldBe true
                    val error = result.leftOrNull().shouldBeInstanceOf<EventPublishingError.StorageFailed>()
                    error.eventId shouldBe event.eventId
                    error.reason shouldBe "Event store error: Database connection failed"
                }

                it("should include event metadata in store command") {
                    // Given
                    val metadata = io.github.kamiazya.scopes.platform.domain.event.EventMetadata(
                        userId = "user-123",
                        correlationId = "corr-456",
                        custom = mapOf("key" to "value"),
                    )
                    val event = createTestProposalCreatedEvent(metadata = metadata)
                    val serializedData = """{"test":"data"}"""

                    every { mockEventTypeMapping.getTypeId(ProposalCreated::class) } returns "collaborative-versioning.proposal.created.v1"
                    every { mockEventSerializer.serialize(event) } returns serializedData.right()
                    coEvery { mockEventStoreCommandPort.createEvent(any()) } returns Unit.right()

                    // When
                    val result = runBlocking { publisher.publish(event) }

                    // Then
                    result.isRight() shouldBe true

                    coVerify {
                        mockEventStoreCommandPort.createEvent(
                            withArg { command ->
                                command.metadata["eventId"] shouldBe event.eventId.toString()
                                command.metadata["userId"] shouldBe "user-123"
                                command.metadata["correlationId"] shouldBe "corr-456"
                                command.metadata["key"] shouldBe "value"
                            },
                        )
                    }
                }
            }

            describe("publishAll") {

                it("should publish multiple events in order") {
                    // Given
                    val event1 = createTestProposalCreatedEvent()
                    val event2 = createTestProposalCreatedEvent()
                    val events = listOf(event1, event2)

                    every { mockEventTypeMapping.getTypeId(ProposalCreated::class) } returns "collaborative-versioning.proposal.created.v1"
                    every { mockEventSerializer.serialize(any()) } returns """{"test":"data"}""".right()
                    coEvery { mockEventStoreCommandPort.createEvent(any()) } returns Unit.right()

                    // When
                    val result = runBlocking { publisher.publishAll(events) }

                    // Then
                    result.isRight() shouldBe true
                    coVerify(exactly = 2) { mockEventStoreCommandPort.createEvent(any()) }
                }

                it("should stop on first failure when publishing multiple events") {
                    // Given
                    val event1 = createTestProposalCreatedEvent()
                    val event2 = createTestProposalCreatedEvent()
                    val events = listOf(event1, event2)

                    every { mockEventTypeMapping.getTypeId(ProposalCreated::class) } returns "collaborative-versioning.proposal.created.v1"
                    every { mockEventSerializer.serialize(event1) } returns """{"test":"data"}""".right()
                    every { mockEventSerializer.serialize(event2) } returns
                        EventStoreError.InvalidEventError(
                            eventType = "collaborative-versioning.proposal.created.v1",
                            validationErrors = listOf(
                                EventStoreError.ValidationIssue(
                                    field = "content",
                                    rule = EventStoreError.ValidationRule.INVALID_TYPE,
                                ),
                            ),
                            occurredAt = Clock.System.now(),
                        ).left()
                    coEvery { mockEventStoreCommandPort.createEvent(any()) } returns Unit.right()

                    // When
                    val result = runBlocking { publisher.publishAll(events) }

                    // Then
                    result.isLeft() shouldBe true
                    result.leftOrNull().shouldBeInstanceOf<EventPublishingError.SerializationFailed>()

                    // First event should have been published, second should not
                    coVerify(exactly = 1) { mockEventStoreCommandPort.createEvent(any()) }
                }
            }
        }
    })

// Helper function to create test events
private fun createTestProposalCreatedEvent(metadata: io.github.kamiazya.scopes.platform.domain.event.EventMetadata? = null): ProposalCreated = ProposalCreated(
    eventId = EventId.generate(),
    aggregateId = AggregateId.from("proposal-aggregate-123").getOrNull() ?: AggregateId.generate(),
    aggregateVersion = AggregateVersion.from(1L).getOrNull() ?: AggregateVersion.initial(),
    occurredAt = Clock.System.now(),
    metadata = metadata,
    proposalId = ProposalId.generate(),
    title = "Test Proposal",
    description = "Test description",
    targetScopeId = "scope-123",
    changeType = "update_title",
    changePayload = mapOf("newTitle" to "Updated Title"),
    createdBy = "user-123",
    tags = listOf("test", "demo"),
)
