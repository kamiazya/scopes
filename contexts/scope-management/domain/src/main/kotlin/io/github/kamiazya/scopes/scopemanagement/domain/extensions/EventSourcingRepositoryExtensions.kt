package io.github.kamiazya.scopes.scopemanagement.domain.extensions

import arrow.core.Either
import io.github.kamiazya.scopes.platform.domain.aggregate.AggregateResult
import io.github.kamiazya.scopes.platform.domain.aggregate.AggregateRoot
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.event.EventEnvelope
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.EventSourcingRepository

/**
 * Extension function to persist an AggregateResult to the repository.
 */
suspend fun <A : AggregateRoot<A, DomainEvent>> EventSourcingRepository<DomainEvent>.persist(
    result: AggregateResult<A, DomainEvent>,
): Either<ScopesError, List<EventEnvelope.Persisted<DomainEvent>>> = saveEventsWithVersioning(
    aggregateId = result.aggregate.id,
    events = result.events,
    expectedVersion = result.baseVersion.value.toInt(),
)
