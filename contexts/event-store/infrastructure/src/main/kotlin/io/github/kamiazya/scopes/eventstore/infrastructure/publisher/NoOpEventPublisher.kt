package io.github.kamiazya.scopes.eventstore.infrastructure.publisher

import io.github.kamiazya.scopes.eventstore.application.port.EventPublisher
import io.github.kamiazya.scopes.eventstore.domain.entity.PersistedEventRecord

/**
 * No-operation implementation of EventPublisher.
 *
 * This implementation does nothing when events are published.
 * It can be used in environments where event publishing is not required.
 */
class NoOpEventPublisher : EventPublisher {
    override suspend fun publish(event: PersistedEventRecord) {
        // No operation - events are stored but not published
    }
}
