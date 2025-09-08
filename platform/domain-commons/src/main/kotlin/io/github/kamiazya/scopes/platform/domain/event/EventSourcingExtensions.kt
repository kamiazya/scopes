package io.github.kamiazya.scopes.platform.domain.event

import io.github.kamiazya.scopes.platform.domain.aggregate.AggregateResult
import io.github.kamiazya.scopes.platform.domain.aggregate.AggregateRoot

/**
 * Extension function to add metadata to all events in an AggregateResult.
 * Note: This requires events to implement MetadataSupport interface or have a copy method with metadata parameter.
 */
fun <A : AggregateRoot<A, E>, E : DomainEvent> AggregateResult<A, E>.withMetadata(metadata: EventMetadata): AggregateResult<A, E> = copy(
    events = events.map { pending ->
        EventEnvelope.Pending(
            when (val event = pending.event) {
                is MetadataSupport<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    event.withMetadata(metadata) as E
                }
                else -> pending.event // Keep original if metadata not supported
            },
        )
    },
)

/**
 * Extension function to evolve an aggregate through a list of persisted events.
 */
fun <A : AggregateRoot<A, E>, E : DomainEvent> A.evolveWithPersisted(events: List<EventEnvelope.Persisted<E>>): A = events.fold(this) { aggregate, envelope ->
    aggregate.applyEvent(envelope.event)
}

/**
 * Extension function to evolve an aggregate through a list of pending events.
 */
fun <A : AggregateRoot<A, E>, E : DomainEvent> A.evolveWithPending(events: List<EventEnvelope.Pending<E>>): A = events.fold(this) { aggregate, envelope ->
    aggregate.applyEvent(envelope.event)
}
