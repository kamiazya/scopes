package io.github.kamiazya.scopes.contracts.eventstore.queries

/**
 * Contract query to retrieve events for an aggregate within a version range (inclusive).
 */
public data class GetEventsByAggregateVersionRangeQuery(val aggregateId: String, val fromVersion: Int, val toVersion: Int, val limit: Int? = null)
