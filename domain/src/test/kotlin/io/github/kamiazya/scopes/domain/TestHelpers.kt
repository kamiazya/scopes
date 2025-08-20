package io.github.kamiazya.scopes.domain

import arrow.core.getOrElse
import io.github.kamiazya.scopes.domain.valueobject.EventId

/**
 * Test helper functions for domain tests.
 */
object TestHelpers {

    /**
     * Create a test EventId for the given event type.
     * This helper simplifies test setup by handling the Either type.
     */
    fun testEventId(eventType: String = "TestEvent"): EventId {
        return EventId.create(eventType).getOrElse {
            throw AssertionError("Failed to create test EventId: $it")
        }
    }
}
