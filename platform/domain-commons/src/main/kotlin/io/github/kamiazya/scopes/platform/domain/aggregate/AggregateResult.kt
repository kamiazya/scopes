package io.github.kamiazya.scopes.platform.domain.aggregate

import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.event.EventEnvelope
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import org.jmolecules.ddd.types.Identifier

/**
 * Represents the result of an aggregate command execution.
 *
 * Contains the updated aggregate, pending events to be persisted, and the base version
 * from which the events were generated.
 *
 * @param A The aggregate type
 * @param ID The identifier type
 * @param E The domain event type
 * @param aggregate The updated aggregate after applying the events
 * @param events The pending events to be persisted
 * @param baseVersion The version of the aggregate before the command was executed
 */
data class AggregateResult<A : AggregateRoot<A, ID, E>, ID : Identifier, E : DomainEvent>(
    val aggregate: A,
    val events: List<EventEnvelope.Pending<E>>,
    val baseVersion: AggregateVersion,
)
