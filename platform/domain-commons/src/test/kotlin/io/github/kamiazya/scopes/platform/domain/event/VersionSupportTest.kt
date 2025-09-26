package io.github.kamiazya.scopes.platform.domain.event

import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

// Test event implementing VersionSupport
data class TestEvent(
    override val eventId: EventId = EventId.generate(),
    override val aggregateId: AggregateId,
    override val aggregateVersion: AggregateVersion = AggregateVersion.initial(),
    override val occurredAt: Instant = Clock.System.now(),
    val data: String
) : DomainEvent, VersionSupport<TestEvent> {
    override fun withVersion(version: AggregateVersion): TestEvent =
        copy(aggregateVersion = version)
}

class VersionSupportTest : DescribeSpec({
    describe("VersionSupport with covariant type parameter") {
        it("should allow casting concrete event to VersionSupport<DomainEvent>") {
            val aggregateId = AggregateId.generate()
            val event: DomainEvent = TestEvent(
                aggregateId = aggregateId,
                data = "test data"
            )
            
            // This cast should work with covariant VersionSupport
            val versionSupport = event as? VersionSupport<DomainEvent>
            versionSupport shouldBe event
            
            // Should be able to call withVersion
            val newVersion = AggregateVersion.fromUnsafe(5L)
            val eventWithVersion = versionSupport?.withVersion(newVersion)
            
            eventWithVersion.shouldBeInstanceOf<TestEvent>()
            eventWithVersion.aggregateVersion shouldBe newVersion
            eventWithVersion.data shouldBe "test data"
        }
    }
})