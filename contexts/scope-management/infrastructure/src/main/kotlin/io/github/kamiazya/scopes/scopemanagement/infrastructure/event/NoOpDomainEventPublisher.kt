package io.github.kamiazya.scopes.scopemanagement.infrastructure.event

import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.scopemanagement.application.port.DomainEventPublisher

/**
 * No-operation implementation of DomainEventPublisher for testing.
 * Simply discards all events without processing.
 */
class NoOpDomainEventPublisher : DomainEventPublisher {

    override suspend fun publish(event: DomainEvent) {
        // No-op: discard event
    }

    override suspend fun publishAll(events: List<DomainEvent>) {
        // No-op: discard all events
    }
}
