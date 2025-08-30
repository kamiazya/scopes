package io.github.kamiazya.scopes.eventstore.application.query

import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import kotlinx.datetime.Instant

/**
 * Query to retrieve events by aggregate ID.
 */
data class GetEventsByAggregate(val aggregateId: AggregateId, val since: Instant? = null, val limit: Int? = null)
