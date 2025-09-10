package io.github.kamiazya.scopes.collaborativeversioning.infrastructure.adapter

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.collaborativeversioning.application.error.EventPublishingError
import io.github.kamiazya.scopes.collaborativeversioning.application.port.DomainEventPublisher
import io.github.kamiazya.scopes.collaborativeversioning.domain.event.ProposalCreated
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ProposalId
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.milliseconds

class RetryingDomainEventPublisherTest :
    DescribeSpec({

        describe("RetryingDomainEventPublisher") {

            val mockDelegate = mockk<DomainEventPublisher>()

            val retryingPublisher = RetryingDomainEventPublisher(
                delegate = mockDelegate,
                maxAttempts = 3,
                baseDelay = 10.milliseconds,
                maxDelay = 100.milliseconds,
                backoffFactor = 2.0,
            )

            beforeEach {
                clearMocks(mockDelegate)
            }

            describe("publish with retry") {

                it("should succeed on first attempt without retry") {
                    // Given
                    val event = createTestProposalCreatedEvent()
                    coEvery { mockDelegate.publish(event) } returns Unit.right()

                    // When
                    val result = runBlocking { retryingPublisher.publish(event) }

                    // Then
                    result.isRight() shouldBe true
                    coVerify(exactly = 1) { mockDelegate.publish(event) }
                }

                it("should retry and succeed on second attempt") {
                    // Given
                    val event = createTestProposalCreatedEvent()
                    val storageError = EventPublishingError.StorageFailed(
                        eventId = event.eventId,
                        eventType = "test.event",
                        reason = "Transient error",
                    )

                    coEvery { mockDelegate.publish(event) } returnsMany listOf(
                        storageError.left(),
                        Unit.right(),
                    )

                    // When
                    val result = runBlocking { retryingPublisher.publish(event) }

                    // Then
                    result.isRight() shouldBe true
                    coVerify(exactly = 2) { mockDelegate.publish(event) }
                }

                it("should fail after max attempts") {
                    // Given
                    val event = createTestProposalCreatedEvent()
                    val storageError = EventPublishingError.StorageFailed(
                        eventId = event.eventId,
                        eventType = "test.event",
                        reason = "Persistent error",
                    )

                    coEvery { mockDelegate.publish(event) } returns storageError.left()

                    // When
                    val result = runBlocking { retryingPublisher.publish(event) }

                    // Then
                    result.isLeft() shouldBe true
                    result.leftOrNull() shouldBe storageError
                    coVerify(exactly = 3) { mockDelegate.publish(event) }
                }

                it("should not retry serialization errors") {
                    // Given
                    val event = createTestProposalCreatedEvent()
                    val serializationError = EventPublishingError.SerializationFailed(
                        eventId = event.eventId,
                        eventType = "test.event",
                        reason = "Invalid data",
                    )

                    coEvery { mockDelegate.publish(event) } returns serializationError.left()

                    // When
                    val result = runBlocking { retryingPublisher.publish(event) }

                    // Then
                    result.isLeft() shouldBe true
                    result.leftOrNull() shouldBe serializationError
                    coVerify(exactly = 1) { mockDelegate.publish(event) }
                }

                it("should not retry unregistered event type errors") {
                    // Given
                    val event = createTestProposalCreatedEvent()
                    val unregisteredError = EventPublishingError.UnregisteredEventType(
                        eventType = "test.event",
                        eventClass = "TestEvent",
                    )

                    coEvery { mockDelegate.publish(event) } returns unregisteredError.left()

                    // When
                    val result = runBlocking { retryingPublisher.publish(event) }

                    // Then
                    result.isLeft() shouldBe true
                    result.leftOrNull() shouldBe unregisteredError
                    coVerify(exactly = 1) { mockDelegate.publish(event) }
                }

                it("should retry timeout errors") {
                    // Given
                    val event = createTestProposalCreatedEvent()
                    val timeoutError = EventPublishingError.PublishTimeout(
                        eventId = event.eventId,
                        eventType = "test.event",
                        timeoutMs = 5000,
                    )

                    coEvery { mockDelegate.publish(event) } returnsMany listOf(
                        timeoutError.left(),
                        Unit.right(),
                    )

                    // When
                    val result = runBlocking { retryingPublisher.publish(event) }

                    // Then
                    result.isRight() shouldBe true
                    coVerify(exactly = 2) { mockDelegate.publish(event) }
                }
            }

            describe("publishAll with retry") {

                it("should publish all events successfully") {
                    // Given
                    val event1 = createTestProposalCreatedEvent()
                    val event2 = createTestProposalCreatedEvent()
                    val events = listOf(event1, event2)

                    coEvery { mockDelegate.publish(any()) } returns Unit.right()

                    // When
                    val result = runBlocking { retryingPublisher.publishAll(events) }

                    // Then
                    result.isRight() shouldBe true
                    coVerify(exactly = 2) { mockDelegate.publish(any()) }
                }

                it("should retry individual events in publishAll") {
                    // Given
                    val event1 = createTestProposalCreatedEvent()
                    val event2 = createTestProposalCreatedEvent()
                    val events = listOf(event1, event2)

                    val transientError = EventPublishingError.StorageFailed(
                        eventId = event1.eventId,
                        eventType = "test.event",
                        reason = "Transient",
                    )

                    coEvery { mockDelegate.publish(event1) } returnsMany listOf(
                        transientError.left(),
                        Unit.right(),
                    )
                    coEvery { mockDelegate.publish(event2) } returns Unit.right()

                    // When
                    val result = runBlocking { retryingPublisher.publishAll(events) }

                    // Then
                    result.isRight() shouldBe true
                    coVerify(exactly = 2) { mockDelegate.publish(event1) }
                    coVerify(exactly = 1) { mockDelegate.publish(event2) }
                }
            }
        }
    })

// Helper function to create test events
private fun createTestProposalCreatedEvent(): ProposalCreated = ProposalCreated(
    eventId = EventId.generate(),
    aggregateId = AggregateId.from("proposal-aggregate-${System.currentTimeMillis()}"),
    aggregateVersion = AggregateVersion.from(1L),
    occurredAt = Clock.System.now(),
    metadata = null,
    proposalId = ProposalId.generate(),
    title = "Test Proposal",
    description = "Test description",
    targetScopeId = "scope-123",
    changeType = "update_title",
    changePayload = mapOf("newTitle" to "Updated Title"),
    createdBy = "user-123",
    tags = listOf("test"),
)
