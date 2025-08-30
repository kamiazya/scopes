package io.github.kamiazya.scopes.eventstore.application.port

import arrow.core.Either
import io.github.kamiazya.scopes.eventstore.domain.error.EventStoreError
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent

/**
 * Port for serializing and deserializing domain events.
 * This abstraction keeps serialization concerns out of the domain layer.
 */
interface EventSerializer {
    /**
     * Serializes a domain event to a string representation.
     *
     * @param event The domain event to serialize
     * @return Either an error or the serialized event data
     */
    fun serialize(event: DomainEvent): Either<EventStoreError.InvalidEventError, String>

    /**
     * Deserializes event data back to a domain event.
     *
     * @param eventType The type of the event
     * @param eventData The serialized event data
     * @return Either an error or the deserialized domain event
     */
    fun deserialize(eventType: String, eventData: String): Either<EventStoreError.InvalidEventError, DomainEvent>
}
