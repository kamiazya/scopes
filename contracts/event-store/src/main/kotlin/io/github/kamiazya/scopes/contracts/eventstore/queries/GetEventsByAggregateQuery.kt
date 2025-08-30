package io.github.kamiazya.scopes.contracts.eventstore.queries

import kotlinx.datetime.Instant

/**
 * Query to retrieve events by aggregate ID.
 */
public data class GetEventsByAggregateQuery(val aggregateId: String, val since: Instant? = null, val limit: Int? = null)
