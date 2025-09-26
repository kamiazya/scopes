package io.github.kamiazya.scopes.contracts.eventstore.queries

/**
 * Contract query to retrieve events for an aggregate from a specific version.
 */
public data class GetEventsByAggregateFromVersionQuery(val aggregateId: String, val fromVersion: Int, val limit: Int? = null)
