package io.github.kamiazya.scopes.eventstore.application.query

import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import kotlinx.datetime.Instant

/**
 * Query to retrieve events by aggregate ID.
 * Follows CQRS naming convention where all queries end with 'Query' suffix.
 */
data class GetEventsByAggregateQuery(val aggregateId: AggregateId, val since: Instant? = null, val limit: Int? = null)
