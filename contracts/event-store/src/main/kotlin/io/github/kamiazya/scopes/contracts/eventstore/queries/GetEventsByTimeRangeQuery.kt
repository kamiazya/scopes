package io.github.kamiazya.scopes.contracts.eventstore.queries

import kotlinx.datetime.Instant

/**
 * Query to retrieve events within a time range.
 * @property from Start time (inclusive)
 * @property to End time (exclusive)
 * @property limit Maximum number of events to return
 * @property offset Number of events to skip
 */
public data class GetEventsByTimeRangeQuery(public val from: Instant, public val to: Instant, public val limit: Int = 100, public val offset: Int = 0)
