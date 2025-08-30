package io.github.kamiazya.scopes.eventstore.application.command

import io.github.kamiazya.scopes.platform.domain.event.DomainEvent

/**
 * Command to store a domain event.
 */
data class StoreEvent(val event: DomainEvent)
