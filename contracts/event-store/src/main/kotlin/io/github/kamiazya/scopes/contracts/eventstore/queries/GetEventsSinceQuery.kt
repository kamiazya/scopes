package io.github.kamiazya.scopes.contracts.eventstore.queries

import kotlinx.datetime.Instant

/**
 * Query to retrieve events since a specific timestamp.
 */
public data class GetEventsSinceQuery(val since: Instant, val limit: Int? = null)
