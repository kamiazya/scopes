package io.github.kamiazya.scopes.eventstore.application.command

import io.github.kamiazya.scopes.platform.domain.event.DomainEvent

/**
 * Command to store a domain event.
 * Follows CQRS naming convention where all commands end with 'Command' suffix.
 */
data class StoreEventCommand(val event: DomainEvent)
