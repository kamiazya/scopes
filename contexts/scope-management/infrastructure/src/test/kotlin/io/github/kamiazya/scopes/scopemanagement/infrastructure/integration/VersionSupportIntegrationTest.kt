package io.github.kamiazya.scopes.scopemanagement.infrastructure.integration

import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.github.kamiazya.scopes.platform.domain.event.EventTypeId
import io.github.kamiazya.scopes.platform.domain.event.VersionSupport
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

// Test event implementing VersionSupport
@EventTypeId("test.event.created.v1")
data class TestCreatedEvent(
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId,
    override val aggregateVersion: AggregateVersion = AggregateVersion.initial(),
    override val occurredAt: Instant = Clock.System.now(),
    val data: String
) : DomainEvent, VersionSupport<TestCreatedEvent> {
    override fun withVersion(version: AggregateVersion): TestCreatedEvent =
        copy(aggregateVersion = version)
}

// Another test event
@EventTypeId("test.event.updated.v1")
data class TestUpdatedEvent(
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId,
    override val aggregateVersion: AggregateVersion = AggregateVersion.initial(),
    override val occurredAt: Instant = Clock.System.now(),
    val oldValue: String,
    val newValue: String
) : DomainEvent, VersionSupport<TestUpdatedEvent> {
    override fun withVersion(version: AggregateVersion): TestUpdatedEvent =
        copy(aggregateVersion = version)
}

class VersionSupportIntegrationTest : DescribeSpec({
    describe("VersionSupport covariance") {
        it("should allow casting concrete event to VersionSupport<DomainEvent>") {
            val aggregateId = AggregateId.generate()
            val event: DomainEvent = TestCreatedEvent(
                aggregateId = aggregateId,
                data = "test data"
            )
            
            // This cast should work with covariant VersionSupport
            val versionSupport = event as? VersionSupport<DomainEvent>
            versionSupport shouldBe event
            
            // Should be able to call withVersion
            val newVersion = AggregateVersion.fromUnsafe(5L)
            val eventWithVersion = versionSupport?.withVersion(newVersion)
            
            eventWithVersion.shouldBeInstanceOf<TestCreatedEvent>()
            eventWithVersion.aggregateVersion shouldBe newVersion
        }
        
        it("should work with different event types") {
            val aggregateId = AggregateId.generate()
            val events: List<DomainEvent> = listOf(
                TestCreatedEvent(aggregateId = aggregateId, data = "created"),
                TestUpdatedEvent(aggregateId = aggregateId, oldValue = "old", newValue = "new")
            )
            
            events.forEachIndexed { index, event ->
                // Cast to VersionSupport<DomainEvent>
                val versionSupport = event as? VersionSupport<DomainEvent>
                versionSupport shouldBe event
                
                // Apply version
                val version = AggregateVersion.fromUnsafe((index + 1).toLong())
                val eventWithVersion = versionSupport?.withVersion(version)
                
                eventWithVersion?.aggregateVersion shouldBe version
            }
        }
        
        it("should handle events without VersionSupport") {
            // Event that doesn't implement VersionSupport
            val event = object : DomainEvent {
                override val eventId = EventId.generate()
                override val aggregateId = AggregateId.generate()
                override val aggregateVersion = AggregateVersion.initial()
                override val occurredAt = Clock.System.now()
            }
            
            val versionSupport = event as? VersionSupport<DomainEvent>
            versionSupport shouldBe null
        }
    }
})